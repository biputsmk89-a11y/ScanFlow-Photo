Add-Type -AssemblyName System.Drawing

$brainDir = "C:\Users\NATRA23\.gemini\antigravity-ide\brain\e868e1c0-bd2e-4ba3-9666-d6932a57c71f"
$outDir = "D:\SMK Projek\ScanFlow Photo\store_assets"

if (!(Test-Path $outDir)) {
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
}

function Resize-Image {
    param(
        [string]$sourcePath,
        [string]$destPath,
        [int]$targetWidth,
        [int]$targetHeight
    )
    $src = [System.Drawing.Bitmap]::FromFile($sourcePath)
    $dest = New-Object System.Drawing.Bitmap($targetWidth, $targetHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($dest)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $graphics.DrawImage($src, 0, 0, $targetWidth, $targetHeight)
    $dest.Save($destPath, [System.Drawing.Imaging.ImageFormat]::Png)

    $graphics.Dispose()
    $dest.Dispose()
    $src.Dispose()
    Write-Host "Created $destPath ($targetWidth x $targetHeight)"
}

# 1. High-Res App Icon: 512 x 512
$iconSrc = "$brainDir\app_store_icon_1789278229491.jpg"
if (Test-Path $iconSrc) {
    Resize-Image -sourcePath $iconSrc -destPath "$outDir\icon_512.png" -targetWidth 512 -targetHeight 512
}

# 2. Feature Graphic Banner: 1024 x 500
$bannerSrc = "$brainDir\feature_graphic_banner_1789278251179.jpg"
if (Test-Path $bannerSrc) {
    Resize-Image -sourcePath $bannerSrc -destPath "$outDir\feature_graphic_1024x500.png" -targetWidth 1024 -targetHeight 500
}

# 3. Screenshot 1: Compression
$shot1Src = "$brainDir\screenshot_compress_1789278279451.jpg"
if (Test-Path $shot1Src) {
    Resize-Image -sourcePath $shot1Src -destPath "$outDir\screenshot_1_compress.png" -targetWidth 1080 -targetHeight 1920
}

# 4. Synthesize Screenshot 2: Passport & ID Studio Mockup (1080 x 1920)
$shot2 = New-Object System.Drawing.Bitmap(1080, 1920, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g2 = [System.Drawing.Graphics]::FromImage($shot2)
$g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

# Background gradient
$brushBg = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point(0, 0)),
    (New-Object System.Drawing.Point(0, 1920)),
    [System.Drawing.Color]::FromArgb(11, 15, 25),
    [System.Drawing.Color]::FromArgb(17, 24, 39)
)
$g2.FillRectangle($brushBg, 0, 0, 1080, 1920)

# Headline
$fontTitle = New-Object System.Drawing.Font("Arial", 42, [System.Drawing.FontStyle]::Bold)
$fontSub = New-Object System.Drawing.Font("Arial", 22, [System.Drawing.FontStyle]::Regular)
$brushWhite = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
$brushMuted = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(156, 163, 175))
$brushCyan = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(37, 99, 235))
$brushRed = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(220, 38, 38))
$brushBlue = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(37, 99, 235))
$brushCard = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(31, 41, 55))
$brushCardLight = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(55, 65, 81))

$g2.DrawString("Passport & ID Photo Studio", $fontTitle, $brushWhite, 120, 100)
$g2.DrawString("Official 2x3, 3x4, 4x6 Presets with Smart Backgrounds", $fontSub, $brushMuted, 120, 180)

# Card Container (Phone Frame)
$penCard = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(75, 85, 99), 3)
$g2.FillRectangle($brushCard, 100, 260, 880, 1540)
$g2.DrawRectangle($penCard, 100, 260, 880, 1540)

# Top bar inside card
$g2.DrawString("ID Photo Generator", (New-Object System.Drawing.Font("Arial", 28, [System.Drawing.FontStyle]::Bold)), $brushWhite, 160, 320)

# Spec selector buttons inside card
$g2.FillRectangle($brushBlue, 160, 400, 160, 70)
$g2.DrawString("2 x 3 cm", (New-Object System.Drawing.Font("Arial", 20, [System.Drawing.FontStyle]::Bold)), $brushWhite, 185, 420)

$g2.FillRectangle($brushCardLight, 340, 400, 160, 70)
$g2.DrawString("3 x 4 cm", (New-Object System.Drawing.Font("Arial", 20, [System.Drawing.FontStyle]::Bold)), $brushWhite, 365, 420)

$g2.FillRectangle($brushCardLight, 520, 400, 160, 70)
$g2.DrawString("4 x 6 cm", (New-Object System.Drawing.Font("Arial", 20, [System.Drawing.FontStyle]::Bold)), $brushWhite, 545, 420)

$g2.FillRectangle($brushCardLight, 700, 400, 180, 70)
$g2.DrawString("Custom", (New-Object System.Drawing.Font("Arial", 20, [System.Drawing.FontStyle]::Bold)), $brushWhite, 735, 420)

# Photo preview frame with Red Background
$g2.FillRectangle($brushRed, 300, 520, 480, 640)
$penBorder = New-Object System.Drawing.Pen([System.Drawing.Color]::White, 4)
$g2.DrawRectangle($penBorder, 300, 520, 480, 640)

# Silhouette / face guide
$penGuide = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(200, 255, 255, 255), 3)
$penGuide.DashStyle = [System.Drawing.Drawing2D.DashStyle]::Dash
$g2.DrawEllipse($penGuide, 420, 600, 240, 320)
$g2.DrawString("Face Guidelines & Centering", (New-Object System.Drawing.Font("Arial", 18, [System.Drawing.FontStyle]::Regular)), $brushWhite, 390, 940)

# Color Chips
$g2.DrawString("Select Background Color:", (New-Object System.Drawing.Font("Arial", 22, [System.Drawing.FontStyle]::Bold)), $brushWhite, 160, 1220)
$g2.FillEllipse($brushRed, 160, 1280, 60, 60)
$g2.FillEllipse($brushBlue, 250, 1280, 60, 60)
$g2.FillEllipse((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)), 340, 1280, 60, 60)
$g2.FillEllipse((New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(156, 163, 175))), 430, 1280, 60, 60)

# Multi-photo print layout
$g2.DrawString("Print Layout:", (New-Object System.Drawing.Font("Arial", 22, [System.Drawing.FontStyle]::Bold)), $brushWhite, 160, 1390)
$g2.DrawString("1 Copy  |  2 Copies  |  4 Copies  |  6 Copies  |  8 Copies", (New-Object System.Drawing.Font("Arial", 18, [System.Drawing.FontStyle]::Regular)), $brushMuted, 160, 1440)

# Export button
$g2.FillRectangle($brushBlue, 160, 1540, 760, 100)
$g2.DrawString("Export Print-Ready Sheet (300 DPI)", (New-Object System.Drawing.Font("Arial", 26, [System.Drawing.FontStyle]::Bold)), $brushWhite, 230, 1570)

$shot2.Save("$outDir\screenshot_2_passport.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g2.Dispose()
$shot2.Dispose()
Write-Host "Created $outDir\screenshot_2_passport.png (1080 x 1920)"

# 5. Synthesize Screenshot 3: Multi-Image to PDF & Advanced Tools (1080 x 1920)
$shot3 = New-Object System.Drawing.Bitmap(1080, 1920, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g3 = [System.Drawing.Graphics]::FromImage($shot3)
$g3.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g3.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

$g3.FillRectangle($brushBg, 0, 0, 1080, 1920)
$g3.DrawString("Advanced Image & PDF Tools", $fontTitle, $brushWhite, 120, 100)
$g3.DrawString("Offline Batch Conversion & Multi-Image Document Export", $fontSub, $brushMuted, 120, 180)

# Card Frame
$g3.FillRectangle($brushCard, 100, 260, 880, 1540)
$g3.DrawRectangle($penCard, 100, 260, 880, 1540)

# Tool 1: PDF Studio
$g3.FillRectangle($brushCardLight, 150, 320, 780, 220)
$g3.DrawString("Multi-Image to PDF Converter", (New-Object System.Drawing.Font("Arial", 26, [System.Drawing.FontStyle]::Bold)), $brushWhite, 180, 360)
$g3.DrawString("Combine receipts, photos, and scanned documents into a compressed PDF with customizable page sizes (A4, Letter).", (New-Object System.Drawing.Font("Arial", 18, [System.Drawing.FontStyle]::Regular)), $brushMuted, (New-Object System.Drawing.RectangleF(180, 420, 720, 100)))

# Tool 2: Social & Messaging Presets
$g3.FillRectangle($brushCardLight, 150, 580, 780, 220)
$g3.DrawString("WhatsApp & Social Media Presets", (New-Object System.Drawing.Font("Arial", 26, [System.Drawing.FontStyle]::Bold)), $brushWhite, 180, 620)
$g3.DrawString("One-tap resize for WhatsApp Status, Instagram Post (1:1), Stories (9:16), and YouTube Banners with perfect aspect ratio.", (New-Object System.Drawing.Font("Arial", 18, [System.Drawing.FontStyle]::Regular)), $brushMuted, (New-Object System.Drawing.RectangleF(180, 680, 720, 100)))

# Tool 3: EXIF Privacy Stripper
$g3.FillRectangle($brushCardLight, 150, 840, 780, 220)
$g3.DrawString("EXIF & GPS Privacy Stripper", (New-Object System.Drawing.Font("Arial", 26, [System.Drawing.FontStyle]::Bold)), $brushWhite, 180, 880)
$g3.DrawString("Remove sensitive GPS coordinates, camera serial numbers, and personal timestamps before sharing photos online.", (New-Object System.Drawing.Font("Arial", 18, [System.Drawing.FontStyle]::Regular)), $brushMuted, (New-Object System.Drawing.RectangleF(180, 940, 720, 100)))

# Tool 4: Target File Size
$g3.FillRectangle($brushCardLight, 150, 1100, 780, 220)
$g3.DrawString("Target File Size Optimizer", (New-Object System.Drawing.Font("Arial", 26, [System.Drawing.FontStyle]::Bold)), $brushWhite, 180, 1140)
$g3.DrawString("Specify strict upload limits (e.g., maximum 200 KB or 500 KB) with precision binary search compression.", (New-Object System.Drawing.Font("Arial", 18, [System.Drawing.FontStyle]::Regular)), $brushMuted, (New-Object System.Drawing.RectangleF(180, 1200, 720, 100)))

# Bottom status
$g3.FillRectangle($brushBlue, 150, 1480, 780, 120)
$g3.DrawString("100% Offline  *  Zero Cloud  *  Private", (New-Object System.Drawing.Font("Arial", 26, [System.Drawing.FontStyle]::Bold)), $brushWhite, 220, 1520)

$shot3.Save("$outDir\screenshot_3_tools.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g3.Dispose()
$shot3.Dispose()
Write-Host "Created $outDir\screenshot_3_tools.png (1080 x 1920)"

Write-Host "All Play Store visual assets generated successfully in $outDir"
