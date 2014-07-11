rtER: Real-Time Emergency Response
=======
*a project for the [Mozilla Ignite Challenge](https://mozillaignite.org/)*

Description
-----------
This project deals with the detection, observation, and assessment of situations requiring intervention by emergency responders, offering them access to high-quality "live" data that may be visualized effectively both by responders in-situ and by remote operators in dedicated control rooms. Its components will include multimodal data registration, interactive visualization capabilities, and live streaming of the integrated contents, potentially obtained from a wide range of heterogeneous acquisition devices, including multispectral imaging devices and handheld smartphones.

Links
-----
* [Brainstorming Round Submission](https://mozillaignite.org/ideas/212/)
* [Project Page](https://www.cim.mcgill.ca/sre/projects/rter/)
* [Updates](https://www.cim.mcgill.ca/sre/projects/rter/blog.html)
* [Panoia Street View Library](https://github.com/sparks/panoia)

Developers
----------
The two key folders for you are for the Android rter app in:

	android/rter
	
and the server code (written in Go) in:

	prototype/server
	prototype/videoserver

For the Android project:
* In Eclipse, use 'Import existing Android code', and select the rter directory. You'll see all the sub-projects listed now. Check mark only 'android-websockets', 'bledevices' and 'rter' and hit finish.
* You should now see 3 different projects in your Eclipse Package Explorer view, one for each of the above 3 checkboxes you marked.
* Make sure 'bledevices' and 'rter' are set to compile on the 'Glass Development Preview' Android snapshot (which is Android 4.4) or higher.
* Also make sure 'rter' is using 'bledevices' and 'anroid-websockets' as a Library.
* You should be able to compile and run on any Android device running 4.4 or higher.
