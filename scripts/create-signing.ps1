param(
    [string]$SigningDirectory = (Join-Path $PSScriptRoot '..\..\_android_signing')
)

$ErrorActionPreference = 'Stop'
$signingRoot = [System.IO.Path]::GetFullPath($SigningDirectory)
$keyStore = Join-Path $signingRoot '2048.jks'
$properties = Join-Path $signingRoot '2048.properties'
if ((Test-Path -LiteralPath $keyStore) -or (Test-Path -LiteralPath $properties)) {
    throw '2048 signing files already exist; refusing to replace the release certificate.'
}
if (-not (Test-Path -LiteralPath $signingRoot -PathType Container)) {
    throw "Signing directory does not exist: $signingRoot"
}

$password = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
$env:CERT_PASSWORD = $password
try {
    & keytool -genkeypair -alias game2048 -keyalg RSA -keysize 4096 `
        -sigalg SHA256withRSA -validity 12000 -storetype PKCS12 `
        -dname 'CN=2048, OU=zerocode, O=zerocode, L=shenzhen, ST=guangdong, C=CN' `
        -keystore $keyStore -storepass:env CERT_PASSWORD -keypass:env CERT_PASSWORD -noprompt
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed to create the 2048 certificate.' }
    $content = "storeFile=2048.jks`nstorePassword=$password`nkeyAlias=game2048`nkeyPassword=$password`n"
    [System.IO.File]::WriteAllText($properties, $content, [System.Text.UTF8Encoding]::new($false))
} finally {
    Remove-Item Env:CERT_PASSWORD -ErrorAction SilentlyContinue
}
Write-Output "Created independent 2048 signing files in $signingRoot"
