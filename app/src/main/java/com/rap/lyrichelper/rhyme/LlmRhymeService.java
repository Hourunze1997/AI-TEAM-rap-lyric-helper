package com.rap.lyrichelper.rhyme;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.rap.lyrichelper.data.model.RhymeWord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 大模型在线押韵推荐（Anthropic 兼容 /v1/messages 协议）。
 * Key / BaseURL / 模型名存 SharedPreferences，由押韵面板里的配置对话框维护；
 * 未配置 Key 或请求失败时由调用方回退到本地词典匹配。
 */
public class LlmRhymeService {

    public interface Callback {
        /** words 为 null 表示失败（error 非空）；空列表表示模型没给出有效词 */
        void onResult(List<RhymeWord> words, String error);
    }

    private static final String PREFS = "rhyme_llm_config";
    private static final String DEFAULT_BASE_URL = "https://api.minimaxi.com/anthropic";
    private static final String DEFAULT_MODEL = "glm-5.2";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 是否已配置 Key（决定面板走在线推荐还是本地词典） */
    public static boolean hasKey(Context ctx) {
        return prefs(ctx).getString("api_key", "").trim().length() > 0;
    }

    /** 找押韵词。doubleRhyme=true 时要求双押（末两字韵母都对应）。后台线程请求，主线程回调。 */
    public static void fetchRhymes(Context ctx, String input, boolean doubleRhyme, Callback cb) {
        final String key = prefs(ctx).getString("api_key", "").trim();
        final String base = prefs(ctx).getString("base_url", DEFAULT_BASE_URL).trim();
        final String model = prefs(ctx).getString("model", DEFAULT_MODEL).trim();
        if (key.isEmpty()) {
            cb.onResult(null, "未配置 API Key");
            return;
        }
        final String prompt = buildPrompt(input, doubleRhyme);
        new Thread(() -> {
            try {
                String text = postMessages(base, model, key, prompt);
                List<RhymeWord> words = parseWords(text);
                MAIN.post(() -> cb.onResult(words, words.isEmpty() ? "模型未返回有效词" : null));
            } catch (Exception e) {
                String msg = e.getMessage();
                MAIN.post(() -> cb.onResult(null, msg == null ? "网络请求失败" : msg));
            }
        }).start();
    }

    private static String buildPrompt(String input, boolean doubleRhyme) {
        return "你是中文说唱歌词创作助手。用户给出一个词或一句歌词的结尾，请给出押韵的候选词。\n\n"
                + "要求：\n"
                + "- 押韵类型：" + (doubleRhyme ? "双押（末两个字的韵母都要对应，优先给词组）" : "单押（末字同韵）") + "\n"
                + "- 中文候选的声调尽量与输入末字声调一致，同声调的排前面；note 里标注声调\n"
                + "- **支持中英混押**：英文输入可以给发音相近的中文词，中文输入也可以给发音相近的英文词\n"
                + "  例：输入 fake love → 可给 废话（fake≈fei、love≈hua）、very bad；输入 废话 → 可给 fake love\n"
                + "- 中文输入以中文词为主、穿插少量谐音英文词；英文输入反之\n"
                + "- 给 20~30 个候选，按说唱可用性排序：常用、口语化、适合放进歌词的优先\n"
                + "- note 字段不超过 8 字，说明声调或谐音关系（如\"同调·四声\"、\"谐音 fake\"、\"双押词组\"）\n"
                + "- 只输出 JSON，不要解释、不要代码块标记，格式：\n"
                + "{\"words\":[{\"word\":\"候选词\",\"note\":\"说明\"}]}\n\n"
                + "输入（" + (doubleRhyme ? "双押" : "单押") + "）：" + input;
    }

    /** Anthropic 兼容 /v1/messages 非流式调用，拼接响应里所有 text 块。 */
    private static String postMessages(String base, String model, String key, String prompt) throws Exception {
        if (!base.endsWith("/")) base += "/";
        HttpURLConnection conn = (HttpURLConnection) new URL(base + "v1/messages").openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(45000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", key);
        conn.setRequestProperty("anthropic-version", "2023-06-01");

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", 1500);
        JSONArray msgs = new JSONArray();
        msgs.put(new JSONObject().put("role", "user").put("content", prompt));
        body.put("messages", msgs);
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        OutputStream os = conn.getOutputStream();
        os.write(payload);
        os.close();

        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String resp = new String(readAll(is), StandardCharsets.UTF_8);
        if (code >= 400) {
            throw new IllegalStateException("HTTP " + code + ": "
                    + resp.substring(0, Math.min(200, resp.length())));
        }

        // 思考型模型的响应可能带 thinking 块，只拼接 text 块
        JSONObject root = new JSONObject(resp);
        JSONArray content = root.optJSONArray("content");
        StringBuilder text = new StringBuilder();
        if (content != null) {
            for (int i = 0; i < content.length(); i++) {
                JSONObject block = content.optJSONObject(i);
                if (block != null && "text".equals(block.optString("type"))) {
                    text.append(block.optString("content", ""));
                }
            }
        }
        return text.toString();
    }

    /** 容错解析模型输出：剥掉围栏/前后缀，截取 JSON 体。 */
    private static List<RhymeWord> parseWords(String text) {
        List<RhymeWord> out = new ArrayList<>();
        if (text == null) return out;
        String s = text.trim();
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) return out;
        try {
            JSONArray words = new JSONObject(s.substring(start, end + 1)).optJSONArray("words");
            if (words == null) return out;
            for (int i = 0; i < words.length(); i++) {
                JSONObject w = words.optJSONObject(i);
                if (w == null) continue;
                String word = w.optString("word", "").trim();
                if (word.isEmpty()) continue;
                // rhymeGroupPair 字段复用为说明文案（面板第二行直接展示）
                out.add(new RhymeWord(word, w.optString("note", ""), 1000 - i));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static byte[] readAll(InputStream is) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        is.close();
        return bos.toByteArray();
    }
}
