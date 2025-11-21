# PowerShellで各密度用のアイコンを生成するスクリプト

Add-Type -AssemblyName System.Drawing

$iconPath = "C:\src\projects\flashcard\icon.png"
$baseDir = "C:\src\projects\flashcard\android\app\src\main\res"

# 画像を読み込み
$originalImage = [System.Drawing.Image]::FromFile($iconPath)

Write-Host "元のアイコンサイズ: $($originalImage.Width)x$($originalImage.Height)"

# 各密度用のサイズを定義
$sizes = @{
    "mipmap-mdpi" = 108
    "mipmap-hdpi" = 162
    "mipmap-xhdpi" = 216
    "mipmap-xxhdpi" = 324
    "mipmap-xxxhdpi" = 432
}

# 各密度用のアイコンを生成
foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $outputPath = Join-Path $baseDir "$folder\ic_launcher.png"

    # 新しいビットマップを作成
    $newImage = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($newImage)

    # 高品質リサイズ設定
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    # 画像を描画
    $graphics.DrawImage($originalImage, 0, 0, $size, $size)

    # 保存
    $newImage.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)

    # リソースを解放
    $graphics.Dispose()
    $newImage.Dispose()

    Write-Host "作成: $folder ($size x $size)"
}

# フォアグラウンドレイヤー用（108dpキャンバスで72dpセーフゾーン）
# 1296x1296のキャンバスに864x864の画像を中央配置
$foregroundSize = 1296
$iconSize = 864
$outputPath = Join-Path $baseDir "drawable\ic_launcher_foreground.png"

$foregroundImage = New-Object System.Drawing.Bitmap($foregroundSize, $foregroundSize)
$graphics = [System.Drawing.Graphics]::FromImage($foregroundImage)

# 透明背景
$graphics.Clear([System.Drawing.Color]::Transparent)

# 高品質リサイズ設定
$graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

# 中央に配置
$offset = ($foregroundSize - $iconSize) / 2
$graphics.DrawImage($originalImage, $offset, $offset, $iconSize, $iconSize)

# 保存
$foregroundImage.Save($outputPath, [System.Drawing.Imaging.ImageFormat]::Png)

# リソースを解放
$graphics.Dispose()
$foregroundImage.Dispose()
$originalImage.Dispose()

Write-Host "作成: フォアグラウンドレイヤー ($foregroundSize x $foregroundSize)"
Write-Host "完了！すべてのアイコンが生成されました。"
