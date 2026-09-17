Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\NATRA23\.gemini\antigravity-ide\brain\55af3f5f-a235-4d64-942f-4e2ffbe137af\.user_uploaded\media_1789616055518.jpg"
$src = [System.Drawing.Bitmap]::FromFile($srcPath)

$bgColor = [System.Drawing.Color]::FromArgb(255, 14, 23, 40) # #0E1728

# 1. Base Emblem Crop (Centered at X=289, Y=511, size 380x380)
$emblemCropX = 99
$emblemCropY = 321
$emblemCropSize = 380

$emblemBmp = New-Object System.Drawing.Bitmap($emblemCropSize, $emblemCropSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gE = [System.Drawing.Graphics]::FromImage($emblemBmp)
$gE.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gE.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gE.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gE.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

$srcRect = New-Object System.Drawing.Rectangle($emblemCropX, $emblemCropY, $emblemCropSize, $emblemCropSize)
$destRect = New-Object System.Drawing.Rectangle(0, 0, $emblemCropSize, $emblemCropSize)
$gE.DrawImage($src, $destRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
$gE.Dispose()

# 2. Base Full Logo Crop (X=100, Y=310, W=815, H=402)
$fullCropX = 100
$fullCropY = 310
$fullCropW = 815
$fullCropH = 402

$fullBmp = New-Object System.Drawing.Bitmap($fullCropW, $fullCropH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gF = [System.Drawing.Graphics]::FromImage($fullBmp)
$gF.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gF.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gF.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gF.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

$srcRectFull = New-Object System.Drawing.Rectangle($fullCropX, $fullCropY, $fullCropW, $fullCropH)
$destRectFull = New-Object System.Drawing.Rectangle(0, 0, $fullCropW, $fullCropH)
$gF.DrawImage($src, $destRectFull, $srcRectFull, [System.Drawing.GraphicsUnit]::Pixel)
$gF.Dispose()

# Function to create an icon (square or round)
function Create-Icon {
    param(
        [int]$size,
        [string]$outPath,
        [bool]$isRound = $false,
        [float]$scale = 0.85
    )
    $dir = [System.IO.Path]::GetDirectoryName($outPath)
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

    $canvas = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    if ($isRound) {
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse(0, 0, $size, $size)
        $g.SetClip($path)
    }

    $brush = New-Object System.Drawing.SolidBrush($bgColor)
    $g.FillRectangle($brush, 0, 0, $size, $size)
    $brush.Dispose()

    $targetSize = [int]($size * $scale)
    $offset = [int](($size - $targetSize) / 2)
    $g.DrawImage($emblemBmp, $offset, $offset, $targetSize, $targetSize)

    $canvas.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $canvas.Dispose()
    Write-Host "Created: $outPath ($size x $size, round=$isRound)"
}

# Function to create adaptive icon foreground (432x432, safe zone diameter 260px)
function Create-AdaptiveForeground {
    param([int]$size, [string]$outPath)
    $dir = [System.IO.Path]::GetDirectoryName($outPath)
    if (!(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

    $canvas = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $brush = New-Object System.Drawing.SolidBrush($bgColor)
    $g.FillRectangle($brush, 0, 0, $size, $size)
    $brush.Dispose()

    # Emblem safe zone: 66% of 108dp. In 432px, safe zone is 285px.
    # We set target emblem size to 255px (approx 59% of canvas), perfectly centered.
    $targetSize = [int]($size * 0.60)
    $offset = [int](($size - $targetSize) / 2)
    $g.DrawImage($emblemBmp, $offset, $offset, $targetSize, $targetSize)

    $canvas.Save($outPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose()
    $canvas.Dispose()
    Write-Host "Created Adaptive Foreground: $outPath ($size x $size)"
}

$resDir = "D:\SMK Projek\ScanFlow Photo\app\src\main\res"

# --- 1. ANDROID MIPMAPS (Legacy / Fallback Launcher Icons) ---
$densities = @(
    @{ name = "mdpi";    size = 48;  fgSize = 108 },
    @{ name = "hdpi";    size = 72;  fgSize = 162 },
    @{ name = "xhdpi";   size = 96;  fgSize = 216 },
    @{ name = "xxhdpi";  size = 144; fgSize = 324 },
    @{ name = "xxxhdpi"; size = 192; fgSize = 432 }
)

foreach ($d in $densities) {
    $mDir = "$resDir\mipmap-$($d.name)"
    Create-Icon -size $d.size -outPath "$mDir\ic_launcher.png" -isRound $false -scale 0.88
    Create-Icon -size $d.size -outPath "$mDir\ic_launcher_round.png" -isRound $true -scale 0.88
}

# --- 2. ADAPTIVE FOREGROUND (Drawable densities) ---
foreach ($d in $densities) {
    $drawDir = "$resDir\drawable-$($d.name)"
    Create-AdaptiveForeground -size $d.fgSize -outPath "$drawDir\ic_launcher_foreground.png"
}
# Default fallback drawable foreground (xxxhdpi 432x432)
Create-AdaptiveForeground -size 432 -outPath "$resDir\drawable\ic_launcher_foreground.png"

# --- 3. IN-APP LOGO (ic_scanflow_logo.png) ---
# Replace ic_scanflow_logo with the emblem
Create-Icon -size 512 -outPath "$resDir\drawable\ic_scanflow_logo.png" -isRound $false -scale 0.90

# --- 4. GOOGLE PLAY STORE ASSETS ---
$storeDir = "D:\SMK Projek\ScanFlow Photo\store_assets"
Create-Icon -size 512 -outPath "$storeDir\icon_512.png" -isRound $false -scale 0.85

# Feature graphic: 1024 x 500
$featW = 1024; $featH = 500
$featBmp = New-Object System.Drawing.Bitmap($featW, $featH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gFeat = [System.Drawing.Graphics]::FromImage($featBmp)
$gFeat.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gFeat.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gFeat.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gFeat.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$brushFeat = New-Object System.Drawing.SolidBrush($bgColor)
$gFeat.FillRectangle($brushFeat, 0, 0, $featW, $featH)
$brushFeat.Dispose()
$targetH = 280
$targetW = [int]($fullCropW * ($targetH / $fullCropH))
$featOffsetX = [int](($featW - $targetW) / 2)
$featOffsetY = [int](($featH - $targetH) / 2)
$gFeat.DrawImage($fullBmp, $featOffsetX, $featOffsetY, $targetW, $targetH)
$featBmp.Save("$storeDir\feature_graphic_1024x500.png", [System.Drawing.Imaging.ImageFormat]::Png)
$gFeat.Dispose()
$featBmp.Dispose()
Write-Host "Created Store Feature Graphic: 1024 x 500"

# --- 5. GITHUB ASSETS (.github/assets) ---
$githubAssetsDir = "D:\SMK Projek\ScanFlow Photo\.github\assets"
if (!(Test-Path $githubAssetsDir)) { New-Item -ItemType Directory -Force -Path $githubAssetsDir | Out-Null }

# GitHub App Avatar / Profile Icon: 512 x 512
Create-Icon -size 512 -outPath "$githubAssetsDir\icon.png" -isRound $false -scale 0.85

# GitHub Full Logo (tight with clean margin): W=900, H=440
$logoW = 900; $logoH = 440
$logoBmp = New-Object System.Drawing.Bitmap($logoW, $logoH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gLogo = [System.Drawing.Graphics]::FromImage($logoBmp)
$gLogo.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gLogo.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gLogo.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gLogo.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$brushLogo = New-Object System.Drawing.SolidBrush($bgColor)
$gLogo.FillRectangle($brushLogo, 0, 0, $logoW, $logoH)
$brushLogo.Dispose()
$logoDrawH = 360
$logoDrawW = [int]($fullCropW * ($logoDrawH / $fullCropH))
$logoOffsetX = [int](($logoW - $logoDrawW) / 2)
$logoOffsetY = [int](($logoH - $logoDrawH) / 2)
$gLogo.DrawImage($fullBmp, $logoOffsetX, $logoOffsetY, $logoDrawW, $logoDrawH)
$logoBmp.Save("$githubAssetsDir\logo.png", [System.Drawing.Imaging.ImageFormat]::Png)
$gLogo.Dispose()
$logoBmp.Dispose()
Write-Host "Created GitHub Logo: $githubAssetsDir\logo.png (900 x 440)"

# GitHub Header Banner / Social Preview: 1280 x 640
$bannerW = 1280; $bannerH = 640
$bannerBmp = New-Object System.Drawing.Bitmap($bannerW, $bannerH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$gBanner = [System.Drawing.Graphics]::FromImage($bannerBmp)
$gBanner.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gBanner.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gBanner.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$gBanner.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$brushBanner = New-Object System.Drawing.SolidBrush($bgColor)
$gBanner.FillRectangle($brushBanner, 0, 0, $bannerW, $bannerH)
$brushBanner.Dispose()

# Draw full logo centered in banner
$bDrawH = 340
$bDrawW = [int]($fullCropW * ($bDrawH / $fullCropH))
$bOffsetX = [int](($bannerW - $bDrawW) / 2)
$bOffsetY = [int](($bannerH - $bDrawH) / 2)
$gBanner.DrawImage($fullBmp, $bOffsetX, $bOffsetY, $bDrawW, $bDrawH)

$bannerBmp.Save("$githubAssetsDir\banner.png", [System.Drawing.Imaging.ImageFormat]::Png)
$gBanner.Dispose()
$bannerBmp.Dispose()
Write-Host "Created GitHub Banner: $githubAssetsDir\banner.png (1280 x 640)"

$emblemBmp.Dispose()
$fullBmp.Dispose()
$src.Dispose()

Write-Host "All assets generated successfully!"
