param(
    [string]$Source = "",
    [string]$OutputDirectory = ""
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if (-not $Source) {
    $Source = Join-Path $projectRoot "assets\skyteam-icon.png"
}
if (-not $OutputDirectory) {
    $OutputDirectory = Join-Path $projectRoot "assets"
}

$outputPng = Join-Path $OutputDirectory "skyteam-icon.png"
$outputIco = Join-Path $OutputDirectory "skyteam-icon.ico"

if (-not (Test-Path $Source)) {
    throw "Quellbild nicht gefunden: $Source"
}

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
Add-Type -AssemblyName System.Drawing

function Get-WhiteCircleBounds {
    param([System.Drawing.Bitmap]$Bitmap)

    $minX = $Bitmap.Width
    $minY = $Bitmap.Height
    $maxX = 0
    $maxY = 0
    $found = $false
    $step = 4

    for ($y = 0; $y -lt $Bitmap.Height; $y += $step) {
        for ($x = 0; $x -lt $Bitmap.Width; $x += $step) {
            $pixel = $Bitmap.GetPixel($x, $y)
            $brightness = [int]$pixel.R + [int]$pixel.G + [int]$pixel.B
            if ($brightness -gt 650) {
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
                $found = $true
            }
        }
    }

    if (-not $found) {
        throw "Der weisse Kreis konnte im Quellbild nicht erkannt werden."
    }

    return [pscustomobject]@{
        X = $minX
        Y = $minY
        Width = $maxX - $minX
        Height = $maxY - $minY
    }
}

function New-IconBitmap {
    param(
        [System.Drawing.Bitmap]$SourceBitmap,
        [System.Drawing.Rectangle]$Crop,
        [int]$Size
    )

    $bitmap = New-Object System.Drawing.Bitmap $Size, $Size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath

    try {
        $graphics.Clear([System.Drawing.Color]::Transparent)
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $path.AddEllipse(0, 0, $Size - 1, $Size - 1)
        $graphics.SetClip($path)
        $destination = New-Object System.Drawing.Rectangle 0, 0, $Size, $Size
        $graphics.DrawImage($SourceBitmap, $destination, $Crop, [System.Drawing.GraphicsUnit]::Pixel)
    } finally {
        $path.Dispose()
        $graphics.Dispose()
    }

    return $bitmap
}

function ConvertTo-PngBytes {
    param([System.Drawing.Bitmap]$Bitmap)

    $stream = New-Object System.IO.MemoryStream
    $Bitmap.Save($stream, [System.Drawing.Imaging.ImageFormat]::Png)
    return $stream.ToArray()
}

function Save-Ico {
    param(
        [string]$Path,
        [byte[][]]$PngImages,
        [int[]]$Sizes
    )

    $fileStream = [System.IO.File]::Create($Path)
    $writer = New-Object System.IO.BinaryWriter $fileStream

    try {
        $writer.Write([UInt16]0)
        $writer.Write([UInt16]1)
        $writer.Write([UInt16]$PngImages.Count)

        $offset = 6 + (16 * $PngImages.Count)
        for ($i = 0; $i -lt $PngImages.Count; $i++) {
            $sizeByte = if ($Sizes[$i] -eq 256) { 0 } else { $Sizes[$i] }
            $writer.Write([byte]$sizeByte)
            $writer.Write([byte]$sizeByte)
            $writer.Write([byte]0)
            $writer.Write([byte]0)
            $writer.Write([UInt16]1)
            $writer.Write([UInt16]32)
            $writer.Write([UInt32]$PngImages[$i].Length)
            $writer.Write([UInt32]$offset)
            $offset += $PngImages[$i].Length
        }

        foreach ($image in $PngImages) {
            $writer.Write($image)
        }
    } finally {
        $writer.Dispose()
        $fileStream.Dispose()
    }
}

$sourceBytes = [System.IO.File]::ReadAllBytes($Source)
$sourceStream = New-Object System.IO.MemoryStream -ArgumentList (,$sourceBytes)
$sourceBitmap = [System.Drawing.Bitmap]::FromStream($sourceStream)
try {
    $bounds = Get-WhiteCircleBounds -Bitmap $sourceBitmap
    $centerX = $bounds.X + ($bounds.Width / 2)
    $centerY = $bounds.Y + ($bounds.Height / 2)
    $side = [Math]::Max($bounds.Width, $bounds.Height)
    $side = [int][Math]::Round($side * 1.02)
    $cropX = [int][Math]::Round($centerX - ($side / 2))
    $cropY = [int][Math]::Round($centerY - ($side / 2))

    if ($cropX -lt 0) { $cropX = 0 }
    if ($cropY -lt 0) { $cropY = 0 }
    if (($cropX + $side) -gt $sourceBitmap.Width) { $cropX = $sourceBitmap.Width - $side }
    if (($cropY + $side) -gt $sourceBitmap.Height) { $cropY = $sourceBitmap.Height - $side }

    $crop = New-Object System.Drawing.Rectangle $cropX, $cropY, $side, $side

    $preview = New-IconBitmap -SourceBitmap $sourceBitmap -Crop $crop -Size 512
    try {
        $preview.Save($outputPng, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $preview.Dispose()
    }

    $sizes = @(256, 128, 64, 48, 32, 16)
    $pngImages = New-Object 'System.Collections.Generic.List[byte[]]'
    foreach ($size in $sizes) {
        $iconBitmap = New-IconBitmap -SourceBitmap $sourceBitmap -Crop $crop -Size $size
        try {
            $pngImages.Add((ConvertTo-PngBytes -Bitmap $iconBitmap))
        } finally {
            $iconBitmap.Dispose()
        }
    }

    Save-Ico -Path $outputIco -PngImages $pngImages.ToArray() -Sizes $sizes
} finally {
    $sourceBitmap.Dispose()
    $sourceStream.Dispose()
}

Write-Host "Icon-PNG erstellt: $outputPng"
Write-Host "Icon-ICO erstellt: $outputIco"
