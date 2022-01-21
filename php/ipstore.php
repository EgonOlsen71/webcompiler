<?php
	$token=$_GET["token"];
	if ($token!="19786b73eac3c6fa5c2fe037e4be2edc") {
		echo("Hello world!");
		die();
	}
	file_put_contents(".ip", $_SERVER['REMOTE_ADDR'], LOCK_EX);
	echo("ok");
?>