.PHONY: build clean test

build:
	chmod +x gradlew
	./gradlew assembleDebug --no-daemon --stacktrace

release:
	chmod +x gradlew
	./gradlew assembleRelease --no-daemon --stacktrace

clean:
	chmod +x gradlew
	./gradlew clean --no-daemon

test:
	chmod +x gradlew
	./gradlew test --no-daemon --stacktrace
