<?php
header('Content-Type: text/html; charset=UTF-8');
$apiBase = getenv('API_BASE_URL') ?: 'http://localhost:8080/api';

function asset_version(string $path): string
{
    $fullPath = __DIR__ . DIRECTORY_SEPARATOR . $path;
    return is_file($fullPath) ? (string) filemtime($fullPath) : '1';
}
?>
<!doctype html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SkyTeam Flight School</title>
    <link rel="icon" href="assets/favicon.ico?v=<?= asset_version('assets/favicon.ico') ?>" sizes="any">
    <link rel="apple-touch-icon" href="assets/logo.png?v=<?= asset_version('assets/logo.png') ?>">
    <link rel="stylesheet" href="assets/styles.css?v=<?= asset_version('assets/styles.css') ?>">
    <script src="assets/api.js?v=<?= asset_version('assets/api.js') ?>" defer charset="utf-8"></script>
    <script src="assets/app.js?v=<?= asset_version('assets/app.js') ?>" defer charset="utf-8"></script>
</head>
<body data-api-base="<?= htmlspecialchars($apiBase, ENT_QUOTES, 'UTF-8') ?>">
    <noscript>Diese Anwendung benoetigt JavaScript.</noscript>
    <div id="app"></div>
</body>
</html>
