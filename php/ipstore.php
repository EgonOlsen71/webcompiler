<?php
	$token=$_GET["token"];
	$md5=md5($_SERVER['DOCUMENT_ROOT']);
	if ($token!=$md5) {
		echo("Hello world!");
		die();
	}
	file_put_contents(".ip", $_SERVER['REMOTE_ADDR'], LOCK_EX);
	echo("ok");
?>