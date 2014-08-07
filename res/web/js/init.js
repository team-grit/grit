/*
 * These two will be used as Storage for what to do when the back or the
 * refresh button are pressed
 */
var backAction = function() {
	courseList();
};
var resfreshAction = function() {
	courseList();
};

/**
 * This function is responsible for initializing the whole UI. It will be
 * called when the DOM is fully loaded.
 * 
 * @returns {undefined}
 */
function initialize() {

	// Set an alert of the error sent from the server as the default error
	// handling.
	$.ajaxSetup({
		error: function(xhr, status, error) {
			message = xhr.responseText + "\n(" + error + ")";
			alert(message);
			$(".overlay").css("display", "none");
		}
	});

	// Set an event for clicking a home button (one of the logos).
	$(".home-button").click(function() {
		$("#overlay-loading").css("display", "block");
		courseList();
		return false;
	});

	// Set an event for clicking the back button.
	$(".back-button").click(function() {
		$("#overlay-loading").css("display", "block");
		backAction();
		return false;
	});

	// Set an event for clicking the refresh button.
	$(".refresh-button").click(function() {
		$("#overlay-loading").css("display", "block");
		refreshAction();
		return false;
	});

	// Set an event for clicking the settings button.
	$(".settings-button").click(function() {
		$("#overlay-loading").css("display", "block");
		settings();
		return false;
	});

	// Set an event for clicking the xml button.
	$(".xml-button").click(function() {
		$("#overlay-loading").css("display", "block");
		xml();
		return false;
	});

	// Set an event for clicking the help button.
	$(".help-button").click(function() {
		help();
		return false;
	});

	// Set events inside the course views.
	$("#course-new-button").click(function() {
		courseNew();
		return false;
	});
	$("#course-new form").unbind('submit').submit(function() {
		$.post('/course/create', $("#course-new form").serialize(), courseList);
		return false;
	});

	// Set events for the forms in the XML view.
	$("#xml-config form").unbind('submit').submit(function() {
		$("#overlay-reboot").css("display", "block");
		$.post('xml/update/config', $("#xml-config form").serialize(), xml);
		return false;
	});
	$("#xml-state form").unbind('submit').submit(function() {
		$("#overlay-reboot").css("display", "block");
		$.post('xml/update/state', $("#xml-state form").serialize(), xml);
		return false;
	});

	// Initialize a date and time picker for date and time input fields.
	$('.datetime').datetimepicker({
		format: 'Y-m-d H:i',
		defaultTime: '00:00',
		step: 30,
		roundtime: 'floor'
	});
	$('.timeperiod').datetimepicker({
		format: 'H:i',
		minTime: '00:30',
		maxTime: '06:01',
		defaultTime: '00:30',
		step: 30,
		datepicker: false,
		roundtime: 'floor'
	});

	// Run the courseList action to show an overview of courses at startup.
	courseList();
}

/**
 * This is the view function for the course list action.
 * 
 * @returns {undefined}
 */
function courseList() {
	$("#overlay-loading").css("display", "block");
	$.getJSON('course/list', function(courses) {

		backAction = function() {
			courseList();
		};
		refreshAction = function() {
			courseList();
		};

		$("#course-list #courses").empty();
		$("#content-body > div").css("display", "none");
		$.each(courses, function(i, course) {
			$("#course-list #courses").append('<div class="course">'
					+ '<div class="course-buttons">'
					+ '<a class="icon-button edit" id="course-'
					+ course.id + '-edit" href="course-' + course.id
					+ '-edit"><img src="img/32/pen.png"></a>'
					+ '<a class="icon-button delete" id="course-'
					+ course.id + '-delete" href="course-'
					+ course.id + '-delete"><img src="img/32/del.png"></a>' + '</div>'
					+ '<a id="course-'
					+ course.id + '-show" href="course-'
					+ course.id + '-show" class="show"><img src="img/folder.png">'
					+ '<p class="course_text">' + course.name + '</p></a></div>');
			$("#course-" + course.id + "-edit").click(function() {
				courseEdit(course.id);
				return false;
			});
			$("#course-" + course.id + "-delete").click(function() {
				courseDelete(course.id);
				return false;
			});
			$("#course-" + course.id + "-show").click(function() {
				courseShow(course.id);
				return false;
			});
		});
		$("#course-list").css("display", "block");
		$("#overlay-loading").css("display", "none");
	});
}

/**
 * This is the view function for the course show action. It calls the exercise
 * list for the respective course.
 * 
 * @param {int} courseId
 * @returns {undefined}
 */
function courseShow(courseId) {
	$("#overlay-loading").css("display", "block");
	$.getJSON('course/read/' + courseId, function(course) {

		$("#exercise-new-button").unbind('click').click(function() {
			exerciseNew(courseId);
			return false;
		});

		$("#exercise-new form").unbind('submit').submit(function(e) {
			$.ajax({
				url: 'exercise/create/' + courseId,
				type: 'POST',
				data: new FormData(this),
				processData: false,
				contentType: false,
				success: function() {
					exerciseList(courseId);
				}
			});
			e.preventDefault();
		});
		
		$("#course-show-courseName").html(course.name);

		$("#content-body > div").css("display", "none");
		$("#course-show").css("display", "block");
		exerciseList(courseId);
	});
}

/**
 * This is the view function for the exercise list action.
 * 
 * @param {int} courseId
 * @returns {undefined}
 */
function exerciseList(courseId) {
	$("#overlay-loading").css("display", "block");
	$.getJSON('exercise/list/' + courseId, function(exercises) {

		backAction = function() {
			courseList();
		};
		refreshAction = function() {
			exerciseList(courseId);
		};

		$("#course-show > div").css("display", "none");

		$("#exercise-list table").html('<tr>'
				+ '<th>exercise name</th><th>start</th><th>deadline</th><th>status</th><th></th>'
				+ '</tr>');
		$.each(exercises, function(i, exercise) {
			$("#exercise-list table").append('<tr><td>' + exercise.context.name
					+ '</td>' + '<td>' + exercise.context.startTimeString + '</td>' + '<td>'
					+ exercise.context.deadlineString + '</td>' + '<td>' + exercise.status
					+ '</td>' + '<td>' + '<a class="icon-button edit" id="exercise-'
					+ exercise.id
					+ '-edit" href="exercise-' + exercise.id
					+ '-edit"><img src="img/32/pen.png"></a>'
					+ '<a class="icon-button delete" id="exercise-' + exercise.id
					+ '-delete" href="exercise-'
					+ exercise.id + '-delete"><img src="img/32/del.png"></a>'
					+ '<a class="icon-button download" id="exercise-' + exercise.id
					+ '-download" href="exercise-'
					+ exercise.id + '-download"><img src="img/32/dlpdf.png"></a>'
					+ '</td></tr>');
			$("#exercise-" + exercise.id + "-edit").click(function() {
				exerciseEdit(courseId, exercise.id);
				return false;
			});
			$("#exercise-" + exercise.id + "-delete").click(function() {
				exerciseDelete(courseId, exercise.id);
				return false;
			});
			$("#exercise-" + exercise.id + "-download").css("display", "none");
			if (exercise.status == "ready for download")
				$("#exercise-" + exercise.id + "-download").css("display", "block");
			$("#exercise-" + exercise.id + "-download").click(function() {
				window.open("pdf/course-" + courseId + "/exercise-" + exercise.id
						+ "/report.pdf");
				return false;
			});
		});

		$("#exercise-list").css("display", "block");
		$("#overlay-loading").css("display", "none");
	});
}

/**
 * Not needed and not implemented yet.
 *  
 * @param {int} courseId
 * @param {int} exerciseId
 * @returns {undefined}
 */
function exerciseShow(courseId, exerciseId) {
	$.getJSON('exercise/read/' + courseId + '/' + exerciseId, function(exercise) {

		backAction = function() {
			exerciseList(courseId);
		};
		refreshAction = function() {
			exerciseShow(courseId, exerciseId);
		};
	});
}

/**
 * This is the view function for the exercise new action.
 * 
 * @param {int} courseId
 * @returns {undefined}
 */
function exerciseNew(courseId) {
	$("#overlay-loading").css("display", "block");

	$.getJSON('connection/list', function(connections) {
		$.getJSON('exercise/types', function(exerciseTypes) {

			backAction = function() {
				exerciseList(courseId);
			};
			refreshAction = function() {
				exerciseNew(courseId);
			};

			$("#course-show > div").css("display", "none");

			$("#exercise-new-languageType").empty();
			var typeCount = 0;
			$.each(exerciseTypes, function(i, type) {
				$("#exercise-new-languageType").append('<option value="' + type + '">'
						+ type + '</option>');
				typeCount++;
			});

			$("#exercise-new-connectionId").empty();

			var connectionCount = 0;
			$.each(connections, function(i, connection) {
				$("#exercise-new-connectionId").append('<option value="' + connection.id
						+ '">' + connection.name + '</option>');
				connectionCount++;
			});

			$("#exercise-new-exerciseName").val("");
			$("#exercise-new-start").val("");
			$("#exercise-new-deadline").val("");
			$("#exercise-new-period").val("00:30");
			$("#exercise-new-testfile").val("");

			if (connectionCount == 0) {
				$("#no-connection-error").css("display", "block");
			} else {
				$("#exercise-new").css("display", "block");
			}

			$("#overlay-loading").css("display", "none");
		});

	});
}

/**
 * This is the view function for the exercise edit action.
 * 
 * @param {int} courseId
 * @param {int} exerciseId
 * @returns {undefined}
 */
function exerciseEdit(courseId, exerciseId) {
	$("#overlay-loading").css("display", "block");

	$.getJSON('connection/list', function(connections) {
		$.getJSON('exercise/types', function(exerciseTypes) {
			$.getJSON('exercise/read/' + courseId + '/' + exerciseId, function(
					exercise) {

				backAction = function() {
					exerciseList(courseId);
				};
				refreshAction = function() {
					exerciseEdit(courseId, exerciseId);
				};

				$("#course-show > div").css("display", "none");

				$("#exercise-edit-languageType").empty();

				var typeCount = 0;
				$.each(exerciseTypes, function(i, type) {
					$("#exercise-edit-languageType").append('<option value="' + type + '">'
							+ type + '</option>');
					typeCount++;
				});

				$("#exercise-edit-connectionId").empty();

				var connectionCount = 0;
				$.each(connections, function(i, connection) {
					$("#exercise-edit-connectionId").append('<option value="' + connection.id
							+ '">' + connection.name + '</option>');
					connectionCount++;
				});

				$("#exercise-edit-exerciseName").val(exercise.context.name);
				$("#exercise-edit-start").val(exercise.context.startTimeString);
				$("#exercise-edit-deadline").val(exercise.context.deadlineString);
				$("#exercise-edit-period").val(exercise.context.periodString);
				$("#exercise-edit-testfile").val("");

				$("#exercise-edit form").unbind('submit').submit(function(e) {
					$.ajax({
						url: 'exercise/update/' + courseId + '/' + exerciseId,
						type: 'POST',
						data: new FormData(this),
						processData: false,
						contentType: false,
						success: function() {
							exerciseList(courseId);
						}
					});
					e.preventDefault();
				});

				if (connectionCount == 0) {
					$("#no-connection-error").css("display", "block");
				} else {
					$("#exercise-edit").css("display", "block");
				}

				$("#overlay-loading").css("display", "none");
			});
		});
	});
}

/**
 * This is the view function for the exercise delete action.
 * 
 * @param {int} courseId
 * @param {int} exerciseId
 * @returns {undefined}
 */
function exerciseDelete(courseId, exerciseId) {
	alert("Warning:\n"
			+ "The exercise will be deleted and will no longer be available!");
	$("#overlay-loading").css("display", "block");
	$.getJSON('exercise/delete/' + courseId + '/' + exerciseId, function(
			exercise) {
		alert('The exercise "' + exercise.context.name
				+ '" was successfully deleted');
		exerciseList(courseId);
	});
}

/**
 * This is the view function for the couse new action.
 * 
 * @returns {courseNew}
 */
function courseNew() {

	backAction = function() {
		courseList();
	};
	refreshAction = function() {
		courseNew();
	};

	$("#course-new-courseName").val("");

	$("#overlay-loading").css("display", "block");
	$("#content-body > div").css("display", "none");
	$("#course-new").css("display", "block");
	$("#overlay-loading").css("display", "none");
}

/**
 * This is the view function for the course edit action.
 * 
 * @param {int} courseId
 * @returns {undefined}
 */
function courseEdit(courseId) {
	$("#overlay-loading").css("display", "block");
	$.getJSON('course/read/' + courseId, function(course) {

		backAction = function() {
			courseList();
		};
		refreshAction = function() {
			courseEdit(courseId);
		};
		$("#course-edit-courseName").val(course.name);

		$("#course-edit form").unbind('submit').submit(function(e) {
			$.ajax({
				url: 'course/update/' + courseId,
				type: 'POST',
				data: new FormData(this),
				processData: false,
				contentType: false,
				success: function() {
					courseList(courseId);
				}
			});
			e.preventDefault();
		});

		$("#content-body > div").css("display", "none");
		$("#course-edit").css("display", "block");
		$("#overlay-loading").css("display", "none");

	});
}

/**
 * This is the view function for the connection delete action.
 * 
 * @param {int} courseId
 * @returns {undefined}
 */
function courseDelete(courseId) {
	alert("Warning:\n"
			+ "The course including all its exercises will be deleted and will no longer be available!");
	$("#overlay-loading").css("display", "block");
	$.getJSON('course/delete/' + courseId, function(
			course) {
		alert('The course "' + course.name
				+ '" was successfully deleted');
		courseList();
	});
}

/**
 * This is the view function for the settings. It displays the config form and
 * calls the connection list.
 * 
 * @returns {undefined}
 */
function settings() {
	$("#overlay-loading").css("display", "block");

	$("#connection-new-button").unbind('click').click(function() {
		connectionNew();
		return false;
	});

	$("#connection-new form").unbind('submit').submit(function(e) {
		$.ajax({
			url: 'connection/create',
			type: 'POST',
			data: new FormData(this),
			processData: false,
			contentType: false,
			success: function() {
				connectionList();
			}
		});
		e.preventDefault();
	});

	$("#content-body > div").css("display", "none");
	$("#settings").css("display", "block");
	connectionList();
}

/**
 * This is the view function for the connection list action.
 * 
 * @returns {undefined}
 */
function connectionList() {
	$("#overlay-loading").css("display", "block");
	$.getJSON('connection/list', function(connnections) {

		backAction = function() {
			courseList();
		};
		refreshAction = function() {
			connectionList();
		};

		$("#settings > div").css("display", "none");

		$("#connection-list table").html('<tr>'
				+ '<th>connection name</th><th>type</th><th>location</th><th>username</th><th>SSH username</th><th></th>'
				+ '</tr>');
		$.each(connnections, function(i, connection) {
			$("#connection-list table").append('<tr><td>' + connection.name + '</td>'
					+ '<td>' + connection.connectionType + '</td>' + '<td>'
					+ connection.location + '</td>' + '<td>' + connection.username + '</td>'
					+ '<td>' + connection.sshUsername + '</td>' + '<td>'
					+ '<a class="icon-button edit" id="connection-' + connection.id
					+ '-edit" href="connection-' + connection.id
					+ '-edit"><img src="img/32/pen.png"></a>'
					+ '<a class="icon-button delete" id="connection-' + connection.id
					+ '-delete" href="connection-'
					+ connection.id + '-delete"><img src="img/32/del.png"></a>'
					+ '</td></tr>');
			$("#connection-" + connection.id + "-edit").click(function() {
				connectionEdit(connection.id);
				return false;
			});
			$("#connection-" + connection.id + "-delete").click(function() {
				connectionDelete(connection.id);
				return false;
			});
		});

		$("#connection-list").css("display", "block");
		$("#overlay-loading").css("display", "none");
	});
}

/**
 * This is the view function for the connection new action.
 * 
 * @returns {undefined}
 */
function connectionNew() {
	$("#overlay-loading").css("display", "block");
	$.getJSON('connection/types', function(connectionTypes) {

		backAction = function() {
			connectionList();
		};
		refreshAction = function() {
			connectionNew();
		};

		$("#settings > div").css("display", "none");

		$("#connection-new-connectionName").val("");

		$("#connection-new-connectionType").empty();

		var typeCount = 0;
		$.each(connectionTypes, function(i, type) {
			$("#connection-new-connectionType").append('<option value="' + type + '">'
					+ type + '</option>');
			typeCount++;
		});

		$("#connection-new-location").val("");
		$("#connection-new-username").val("");
		$("#connection-new-password").val("");
		$("#connection-new-sshUsername").val("");
		$("#connection-new-sshKeyFile").val("");
		$("#connection-new-structure").val("");

		$("#connection-new").css("display", "block");
		$("#overlay-loading").css("display", "none");
	});
}

/**
 * This is the view function for the connection edit action.
 * 
 * @returns {undefined}
 */
function connectionEdit(connectionId) {
	$("#overlay-loading").css("display", "block");
	$.getJSON('connection/types', function(connectionTypes) {
		$.getJSON('connection/read/' + connectionId, function(connection) {

			backAction = function() {
				connectionList();
			};
			refreshAction = function() {
				connectionEdit();
			};

			$("#settings > div").css("display", "none");

			$("#connection-edit-connectionName").val(connection.name);

			$("#connection-edit-connectionType").empty();

			var typeCount = 0;
			$.each(connectionTypes, function(i, type) {
				$("#connection-edit-connectionType").append('<option value="' + type
						+ '">'
						+ type + '</option>');
				typeCount++;
			});

			$("#connection-edit-location").val(connection.location);
			$("#connection-edit-username").val(connection.username);
			$("#connection-edit-password").val("");
			$("#connection-edit-sshUsername").val(connection.sshUsername);
			$("#connection-edit-sshKeyFile").val("");
			$("#connection-edit-structure").val(connection.structureString);

			$("#connection-edit form").unbind('submit').submit(function(e) {
				$.ajax({
					url: 'connection/update/' + connectionId,
					type: 'POST',
					data: new FormData(this),
					processData: false,
					contentType: false,
					success: function() {
						connectionList(connectionId);
					}
				});
				e.preventDefault();
			});

			$("#connection-edit").css("display", "block");
			$("#overlay-loading").css("display", "none");
		});
	});
}

/**
 * This is the view function for the connection delete action.
 * 
 * @param {int} connectionId
 * @returns {undefined}
 */
function connectionDelete(connectionId) {
	alert("Warning:\n"
			+ "The connection will be deleted and will no longer be available!");
	$("#overlay-loading").css("display", "block");
	$.getJSON('connection/delete/' + connectionId, function(
			connection) {
		alert('The connection "' + connection.name
				+ '" was successfully deleted');
		connectionList();
	});
}

/**
 * This is the view function for the raw XML editing.
 * 
 * @returns {undefined}
 */
function xml() {
	$("#overlay-loading").css("display", "block");
	$("#overlay-reboot").css("display", "none");

	$.getJSON('xml/read', function(xmls) {
		alert("Warning:\n"
				+ "Modifying the the raw XML can lead to a corrupted state or configuration,"
				+ " resulting in a non-functioning grit.\n"
				+ "Be careful!");

		backAction = function() {
			courseList();
		};
		refreshAction = function() {
			xml();
		};

		$("#xml-config-field").val(xmls[0]);
		$("#xml-state-field").val(xmls[1]);

		$("#content-body > div").css("display", "none");
		$("#xml").css("display", "block");
		$("#overlay-loading").css("display", "none");
	});
}

/**
 * This function opens the documentation.
 * 
 * @returns {undefined}
 */
function help() {
	window.open("doc/grit_documentation.pdf");
}

// Calls initialize when the document is loaded.
$(document).ready(initialize);
