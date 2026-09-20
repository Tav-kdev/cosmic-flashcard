# Commands that worked in this session to set up the Android SDK and build the APK.
# Run this from PowerShell in the project root (D:\Okta\projects\cosmic-android).

# --- 1. JDK 17 (already installed via winget: EclipseAdoptium.Temurin.17.JDK) ---
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"

# --- 2. Lay out the Android SDK properly ---
# The raw cmdline-tools download at D:\Okta\projects\commandlinetools-\cmdline-tools
# needs to live under <sdk_root>\cmdline-tools\latest, not at the sdk root itself.
$sdkRoot = "D:\Okta\android-sdk"
New-Item -ItemType Directory -Force -Path "$sdkRoot\cmdline-tools" | Out-Null
Copy-Item "D:\Okta\projects\commandlinetools-\cmdline-tools" "$sdkRoot\cmdline-tools\latest" -Recurse -Force

$sdkmanager = "$sdkRoot\cmdline-tools\latest\bin\sdkmanager.bat"

# --- 3. Fix TLS handshake failures from sdkmanager ---
# On this machine, PowerShell (Invoke-WebRequest) reaches dl.google.com fine (200 OK),
# but the JDK's own cacerts truststore doesn't trust whatever TLS-terminating proxy/cert
# sits in front of it (common on corporate networks), so sdkmanager fails with
# "IO exception while downloading manifest" / "Failed to download any source lists!".
# Fix: point the JVM at the Windows certificate store instead of its bundled cacerts.
$env:SDKMANAGER_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"

# --- 4. Accept SDK licenses ---
& $sdkmanager --sdk_root="$sdkRoot" --licenses

# --- 5. Install required SDK packages ---
& $sdkmanager --sdk_root="$sdkRoot" "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# --- 6. Point the Gradle project at this SDK ---
Set-Content -Path "D:\Okta\projects\cosmic-android\local.properties" -Value "sdk.dir=D:\\Okta\\android-sdk" -Encoding utf8

# --- 7. Build the debug APK ---
Set-Location "D:\Okta\projects\cosmic-android"
.\gradlew.bat assembleDebug
