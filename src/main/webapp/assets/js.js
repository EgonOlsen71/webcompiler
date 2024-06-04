var webAppPrefix = "/";

var interval=null;
var configName="mospeedconfig";
var proxyPath="mospeed-server";

if (typeof String.prototype.startsWith !== 'function') {
	String.prototype.startsWith = function(suffix) {
		return this.indexOf(suffix) == 0;
	};
}

jQuery(document).ready(function() {
	jQuery("#uploaded").hide(0);
	jQuery("#hiddennames").empty();
	jQuery("#uploadedfiles").empty();

	enableDragAndDrop(jQuery(".uploadform"));

	enableAddressChange("progstart");
	enableAddressChange("varstart");
	enableAddressChange("varend");
	enableAddressChange("runtimestart");

	jQuery("select[name=platform]").change(function() {
		var val=jQuery(this).val();
		if (val=="vic20") {
			jQuery("#memselect").show();
		} else {
			jQuery("#memselect").hide();
		}
		if (val=="c64") {
			jQuery("#ram").show();
		} else {
			jQuery("#ram").hide();
		}
	});

	restoreConfiguration();
});

function saveConfiguration() {
	var conf="";

	jQuery("select option").each(function() {
		var jthis=jQuery(this);
		conf+=(jthis.is(":selected")?"selected":"")+"~";
	});

	jQuery("input").not(":button").each(function() {
		var jthis=jQuery(this);
		if (!jthis.is(":hidden")) {
			if (jthis.is(":radio") || jthis.is(":checkbox")) {
				conf+=(jthis.is(":checked")?"checked":"")+"~";
			} else {
				conf+=jthis.val()+"~";
			}
		}
	});

	Cookies.set(configName, conf, { expires: 30 })
}

function restoreConfiguration() {
	deleteMemHoles();
	var conf=Cookies.get(configName);
	if (conf) {
		var parts=conf.split("~");
		var cnt=0;

		jQuery("select option").each(function() {
			var jthis=jQuery(this);
			var val=parts[cnt++];
			if (val) {
				jthis.attr("selected", val);
				jthis.trigger("change");
			} else {
				jthis.removeAttr("selected");
			}
		});

		jQuery("input").not(":button").each(function() {
			jthis=jQuery(this);
			if (!jthis.is(":hidden")) {
				if (jthis.is(":radio") || jthis.is(":checkbox") ) {
					var val=parts[cnt++];
					if (val) {
						jthis.attr("checked", val);
						jthis.trigger("change");
					} else {
						jthis.removeAttr("checked");
					}
				} else {
					jthis.val(parts[cnt++]);
				}
			}
		});

		for (var i=cnt; i<parts.length-1;) {
			var part=parts[i];
			var newy=addMemoryHole();
			jQuery(newy).find("input").each(function() {
				jQuery(this).val(parts[i++]);
			});
		}
	}
}

function deleteConfiguration() {
	Cookies.remove(configName);
}

function deleteMemHoles() {
	var cnt=0;
	var element=jQuery(".template").each(function() {
		if (cnt>0) {
			jQuery(this).remove();
		}
		cnt++;
	});
}

function resetConfiguration() {
	hide("progstart");
	hide("varstart");
	hide("varend");
	hide("runtimestart");
	hide("memselect");

	deleteMemHoles();
	document.getElementById("myform").reset();
	deleteConfiguration();
	resetSelects();
	resetCheckboxes();

	var val=jQuery("select[name=platform]").val();
	if (val=="vic20") {
		jQuery("#memselect").show();
	} else {
		jQuery("#memselect").hide();
	}
	if (val=="c64") {
		jQuery("#ram").show();
	} else {
		jQuery("#ram").hide();
	}
}

function resetCheckboxes() {
	jQuery("input").each(function() {
		if (jQuery(this).is(":checkbox")) {
			jQuery(this).removeAttr("checked");
		}
	});
}

function resetSelects() {
	jQuery("select option").each(function() {
		jQuery(this).removeAttr("selected");
	});
}

function addMemoryHole() {
	var element=jQuery(".template").last();
	var copy=element.clone();
	jQuery(copy).find("input").val("");
	element.after(copy);
	return copy;
}

function hide(element) {
	var elem=jQuery("#"+element);
	elem.hide(100);
	elem=jQuery("#"+element+"2");
	if (elem) {
		elem.removeAttr("checked");
	}
	elem=jQuery("#"+element+"1");
	if (elem) {
		elem.attr("checked", "checked");
	}
}

function enableAddressChange(element) {
	jQuery("input[name="+element+"default]").change(function() {
		var val=jQuery(this).val();
		if (val==0) {
			jQuery("#"+element).show(100);
			jQuery("#"+element).focus();
		} else {
			jQuery("#"+element).hide(100);
		}
	});
}

function enableDragAndDrop(element) {
	jQuery(element).on('drop', function(e) {
		jQuery(this).css('background-color', '#7C70DA');
		var uploaded = false;
		if (e.originalEvent.dataTransfer) {
			if (e.originalEvent.dataTransfer.files.length) {
				uploaded = true;
				uploadFile(e.originalEvent.dataTransfer.files[0], this);
			}
		}

		if (!uploaded) {
			var txt = e.originalEvent.dataTransfer.getData('Text');
			if (txt != null && txt != '') {
				alert("Please drag a file here, not a text or an URL!");
			}
		}

		e.preventDefault();
		e.stopPropagation();
	});

	jQuery(element).on('dragover', function(e) {
		e.preventDefault();
		e.stopPropagation();
		jQuery(this).css('background-color', '#6BC474');
	});

	jQuery(element).on('dragleave', function(e) {
		e.preventDefault();
		e.stopPropagation();
		jQuery(this).css('background-color', '#7C70DA');
	});
}

function compile() {
	if (jQuery("#hiddennames input").length == 0) {
		alert("Nothing to compile. Please upload at least one source file!");
		return;
	}
	saveConfiguration();
	myform.action="/"+getApplicationPath()+"Compile";
	myform.submit();
	jQuery("#myform").hide(200);
	jQuery("#output").show(200);
	interval=window.setInterval(function() {updateConsole()}, 500);
}

function updateConsole() {
	console.log("Updating console...");
	var text=jQuery("#compiletarget").contents().text();
	if (text.length==0) {
		jQuery("#console").text(jQuery("#console").text()+"...");
		var txt = jQuery("#console").text();
		if (txt.length-txt.lastIndexOf("\n")>40) {
			jQuery("#console").text(jQuery("#console").text()+"\n");
		}
		jQuery(document).scrollTop(jQuery(document).height());
		return;
	}
	var pos=text.indexOf("~~~~");
	var download;
	if (pos!=-1) {
		download = text.substring(pos+5);
		text=text.substring(0,pos);
		window.clearInterval(interval);
		if (download.length>8) {
			download = "/"+getApplicationPath()+"Download?file="+download;
			document.getElementById("compiletarget").src = download;
		} else {
			jQuery("#failed").show();
		}
		jQuery("#runagain").show();
	}

	text = text.replace(/---------------------------------------\n/g, "");
	jQuery("#console").text(text);
	jQuery(document).scrollTop(jQuery(document).height());
}

function storeTmpName(tmpName, fileName) {
	jQuery("#uploadedfiles")
			.append(
					"<li>"
							+ fileName
							+ "  (<a href=\"javascript:void(0)\" onclick=\"deleteFile(event.target, '"
							+ tmpName + "')\">Remove</a>)</li>");
	jQuery("#hiddennames").append(
			"<input name=\"filelist[]\" type=\"hidden\" value=\""
					+ tmpName + "\"/>");
	jQuery("#uploaded").show(200);
}

function deleteFile(element, tmpName) {
	jQuery("#hiddennames input").each(function() {
		if (jQuery(this).val() == tmpName) {
			jQuery(this).remove();
		}
	});
	jQuery(element).parent().remove();
	if (jQuery("#hiddennames input").length == 0) {
		jQuery("#uploaded").hide(100);
	}
	deleteFromServer(tmpName);
}

function uploadSelectedFile(value) {
	var element = jQuery(".uploadform");
	uploadFile(value.files[0], element);
}

function getApplicationPath() {
	var appPath = 'WebCompiler/';
	if (window.location.search.includes('?route=')) {
		appPath=proxyPath+"/?route=";
	}
	return appPath;
}

function uploadFile(file, element) {
	var formData = new FormData();
	formData.append('program', file);
	var jElement = jQuery(element);

	jElement.find('.uploadmsg').html('One moment please...');

	jQuery.ajax(webAppPrefix + getApplicationPath()+'Upload', {
		processData : false,
		contentType : false,
		dataType : 'json',
		method : 'POST',
		cache : false,
		data : formData,

	}).done(
			function(item) {
				if (item.type == 'error') {
					jElement.css('background-color', '#f52020');
					jElement.find('.uploadmsg').html(
							'Unsupported file format!');
				} else {
					jElement.css('background-color', '#7C70DA');
					jElement.find('.uploadmsg').html('Drop file here...');
					storeTmpName(item.text, file.name);
				}
			});
}

function deleteFromServer(fileName) {
	jQuery.ajax({
		method : 'GET',
		cache : false,
		dataType : 'json',
		url : webAppPrefix + getApplicationPath()+'Upload',
		data : {
			"url" : fileName
		}
	}).done(function(item) {
		//
	});
}
