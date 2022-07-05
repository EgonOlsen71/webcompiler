<?php
	$ip = file_get_contents("../.ip");
	if (!$ip) {
		echo("Configuration error!");
		die();
	}
	header("Location: http://".$ip.":8192/WebCompiler/Render/basic");
	die();
?>