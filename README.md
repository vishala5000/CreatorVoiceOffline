# CreatorVoice Offline

A GitHub-ready Android neural TTS app for YouTube creators.

## What it does

- Runs TTS locally on the Android device.
- Ships the Kokoro English model inside the APK.
- Has no `INTERNET` permission.
- Generates WAV audio locally.
- Lets you preview and export the generated audio.
- Supports multiple Kokoro speakers.
- Uses CPU inference through sherpa-onnx.

The app is intentionally offline at runtime. The GitHub Actions build downloads the open-source runtime/model while building the APK; the resulting APK contains the model.

## Architecture

Android app -> sherpa-onnx Kotlin API -> ONNX Runtime/native JNI -> Kokoro ONNX model -> PCM -> WAV.

## Model

Default model: `kokoro-en-v0_19`.

The sherpa-onnx documentation lists this as an English Kokoro model with 11 speakers. See:
https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/kokoro.html

Check the model's included license before distributing an APK commercially.

## Build locally

Use Android Studio with JDK 17.

```bash
./gradlew assembleDebug
```

The local build expects the model directory at:

`app/src/main/assets/kokoro-en-v0_19/`

Run:

```bash
bash scripts/download-model.sh
./gradlew assembleDebug
```

## GitHub Actions

The workflow:

1. Checks out the repository.
2. Downloads the Kokoro model into Android assets.
3. Builds a release APK.
4. Uploads the APK as an Actions artifact.
5. On a version tag, creates a GitHub Release.

No model is committed to Git because it is hundreds of MB and makes repository cloning unnecessarily large.

## Important

"Offline" refers to the installed app's synthesis path. Gradle dependencies and the model are fetched during the build unless you vendor/cache them yourself.
