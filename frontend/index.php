<?php
$apiBase = getenv('API_BASE_URL') ?: 'http://localhost:8080/api';
?>
<!doctype html>
<html lang="de">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>SkyTeam Flight School</title>
    <link rel="stylesheet" href="assets/styles.css">
    <script src="assets/api.js" defer></script>
    <script src="assets/app.js" defer></script>
</head>
<body data-api-base="<?= htmlspecialchars($apiBase, ENT_QUOTES, 'UTF-8') ?>">
    <noscript>Diese Anwendung benoetigt JavaScript.</noscript>
    <div id="app"></div>
</body>
</html>
