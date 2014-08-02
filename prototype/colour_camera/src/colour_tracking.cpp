/*
 * colour_tracking.cpp
 *
 *  Created on: 2011-12-08
 *      Author: dalia
 */

extern "C" {
	#include <pthread.h>
	#include <unistd.h>
}

#include <iostream>
#include <cv.h>
#include <highgui.h>
#include <stdio.h>
#include <cxcore.h>
#include <fstream>
#include <sstream>
#include <string>
#include <sys/time.h>
#include <mutex>
#include "restclient.h"


using namespace std;

const int CAM_ID = 1;
const bool DISPLAY_PREVIEW = true;
const string SERVER_URL = "http://localhost:8000";
const int UPDATE_INTERVAL = 500;




// new values
const double LAT_MIN = 39.072179857392;
const double LAT_MAX = 39.07227720671;
const double LNG_MIN = -94.882592707872;
const double LNG_MAX = -94.882338568568;
/* old small values
const double LAT_MIN = 39.07215;
const double LAT_MAX = 39.07216;
const double LNG_MIN = -94.88228;
const double LNG_MAX = -94.88226;
*/
const double LAT_INTERVAL = LAT_MAX - LAT_MIN;
const double LNG_INTERVAL = LNG_MAX - LNG_MIN;
const int CAPTURE_WIDTH = 1280;
const int CAPTURE_HEIGHT = 720;
const double PREVIEW_SCALE = 0.35;


int get_time_in_msec(){
    time_t ltime;
    struct tm *Tm;
    ltime=time(NULL);
    Tm=localtime(&ltime);
    int hour =Tm -> tm_hour;
    int min = Tm -> tm_min;
    int sec = Tm -> tm_sec;

	struct timeval detail_time;

	gettimeofday(&detail_time,NULL);
	int msec = detail_time.tv_usec /1000;

	int time_in_msec = hour*60*60*1000+min*60*1000+sec*1000+msec;
	return time_in_msec;
}

void  * function1(void * argument);

IplImage* GetThresholdedImageRed(IplImage* img)
{
	//red has a hue of 180
	int red_hue_max_low=35;
	int red_sat_min=160;
	int red_val_min=200;
	int red_hue_min_high=155;
	int red_sat_max=255;
	int red_val_max=255;

	//convert the image into an HSV image
	IplImage* imgHSV = cvCreateImage(cvGetSize(img), 8, 3);
	cvCvtColor(img, imgHSV, CV_BGR2HSV);

	//now we create a new image that will hold the thresholded image
	IplImage* imgThreshed = cvCreateImage(cvGetSize(img), 8, 1);
	IplImage* imgThreshedA = cvCreateImage(cvGetSize(img), 8, 1);
	IplImage* imgThreshedB = cvCreateImage(cvGetSize(img), 8, 1);

	//cvScalar represent the upper and lower bounds of values that are yellow in color
	cvInRangeS(imgHSV, cvScalar(0, red_sat_min, red_val_min), cvScalar(red_hue_max_low, red_sat_max, red_val_max), imgThreshedA);
	cvInRangeS(imgHSV, cvScalar(red_hue_min_high, red_sat_min, red_val_min), cvScalar(180, red_sat_max, red_val_max), imgThreshedB);
	cvOr(imgThreshedA, imgThreshedB, imgThreshed);
	cvReleaseImage(&imgThreshedA);
	cvReleaseImage(&imgThreshedB);

	//release the temporary HSV image and return thresholded image
	cvReleaseImage(&imgHSV);
	return imgThreshed;
}

IplImage* GetThresholdedImageBlue(IplImage* img)
{
	//blue has a hue of 120
	int blue_hue_min=70;
	int blue_sat_min=77;
	int blue_val_min=100;
	int blue_hue_max=175;
	int blue_sat_max=255;
	int blue_val_max=255;

	//convert the image into an HSV image
	IplImage* imgHSV = cvCreateImage(cvGetSize(img), 8, 3);
	cvCvtColor(img, imgHSV, CV_BGR2HSV);

	//now we create a new image that will hold the thresholded image
	IplImage* imgThreshed = cvCreateImage(cvGetSize(img), 8, 1);

	//cvScalar represent the upper and lower bounds of values that are yellow in color
	cvInRangeS(imgHSV, cvScalar(blue_hue_min, blue_sat_min, blue_val_min), cvScalar(blue_hue_max, blue_sat_max, blue_val_max), imgThreshed);

	//release the temporary HSV image and return thresholded image
	cvReleaseImage(&imgHSV);
	return imgThreshed;
}

IplImage* GetThresholdedImageGreen(IplImage* img)
{
	//green has a hue of 60
	int green_hue_min=64;
	int green_sat_min=180;
	int green_val_min=200;
	int green_hue_max=104;
	int green_sat_max=255;
	int green_val_max=255;

	//convert the image into an HSV image
	IplImage* imgHSV = cvCreateImage(cvGetSize(img), 8, 3);
	cvCvtColor(img, imgHSV, CV_BGR2HSV);

	//now we create a new image that will hold the thresholded image
	IplImage* imgThreshed = cvCreateImage(cvGetSize(img), 8, 1);

	//cvScalar represent the upper and lower bounds of values that are yellow in color
	cvInRangeS(imgHSV, cvScalar(green_hue_min, green_sat_min, green_val_min), cvScalar(green_hue_max, green_sat_max, green_val_max), imgThreshed);

	//release the temporary HSV image and return thresholded image
	cvReleaseImage(&imgHSV);
	return imgThreshed;
}

int red_calculations (IplImage* imgRedThresh, ofstream* redfile_ptr, double start_time){
	 //we now calculate the moments to estimate the position of the ball
	 CvMoments *moments_red = (CvMoments*)malloc(sizeof(CvMoments));
	 cvMoments (imgRedThresh, moments_red, 1);

	 double moment10_red = cvGetSpatialMoment(moments_red, 1, 0);
	 double moment01_red = cvGetSpatialMoment(moments_red, 0, 1);
	 double area_red = cvGetCentralMoment(moments_red, 0, 0);

	 static int posX_red = 0;
	 static int posY_red = 0;
	 int current_time, time_since_start;
	 stringstream red_output, red_file;
	 red_output.precision(12);

	 if(area_red!=0){
		 posX_red = moment10_red/area_red;
		 posY_red = moment01_red/area_red;

		 double lat = (((double) posY_red) / ((double) CAPTURE_HEIGHT) * LAT_INTERVAL) + LAT_MIN;
		 double lng = ((1 - ((double) posX_red) / ((double) CAPTURE_WIDTH)) * LNG_INTERVAL) + LNG_MIN;

		 current_time=get_time_in_msec();
		 time_since_start=current_time-start_time;
		 //printf("position of red object(%d, %d)\n", posX_red, posY_red);
		 red_file<< time_since_start << "," << posX_red <<","<< posY_red << ";\n";
		 red_output << "{\"Username\": \"red\", \"Lat\": " << lat << ", \"Lng\": " << lng << "}";
		 RestClient::response r = RestClient::put(SERVER_URL + "/1.0/users/red", "text/json", red_output.str());
		 //cout << "Status: " << r.code << ", " << r.body;
		 //cout << "R: " << lat << ", " << lng << "\n";
		 *redfile_ptr << red_file.str();
	 }



	 return 0;
}

int blue_calculations (IplImage* imgBlueThresh, ofstream* bluefile_ptr, int start_time){
	 //we now calculate the moments to estimate the position of the ball
	 CvMoments *moments_blue = (CvMoments*)malloc(sizeof(CvMoments));
	 cvMoments (imgBlueThresh, moments_blue, 1);

	 double moment10_blue = cvGetSpatialMoment(moments_blue, 1, 0);
	 double moment01_blue = cvGetSpatialMoment(moments_blue, 0, 1);
	 double area_blue = cvGetCentralMoment(moments_blue, 0, 0);

	 static int posX_blue = 0;
	 static int posY_blue = 0;
	 int current_time, time_since_start;

	 stringstream blue_output, blue_file;
	 blue_output.precision(12);
	 cout.precision(12);

	 if(area_blue!=0){
		 posX_blue = moment10_blue/area_blue;
		 posY_blue = moment01_blue/area_blue;

		 double lat = (((double) posY_blue) / ((double) CAPTURE_HEIGHT) * LAT_INTERVAL) + LAT_MIN;
		 double lng = ((1 - ((double) posX_blue) / ((double) CAPTURE_WIDTH)) * LNG_INTERVAL) + LNG_MIN;

		 current_time=get_time_in_msec();
		 time_since_start=current_time-start_time;
		 //printf("position of blue object(%d, %d)\n", posX_blue, posY_blue);
		 blue_file<<time_since_start<<"," << posX_blue <<","<< posY_blue << ";\n";
		 blue_output << "{\"Username\": \"blue\", \"Lat\": " << lat << ", \"Lng\": " << lng << "}";
		 RestClient::response r = RestClient::put(SERVER_URL + "/1.0/users/blue", "text/json", blue_output.str());
		 //cout << "Status: " << r.code << ", " << r.body;
		 cout << "B: " << posX_blue << ", " << posY_blue << "  " << lat << ", " << lng << "\n";
		 *bluefile_ptr << blue_file.str();
	 }



	 return 0;
}

int green_calculations (IplImage* imgGreenThresh, ofstream* greenfile_ptr, int start_time){
	 //we now calculate the moments to estimate the position of the ball
	 CvMoments *moments_green = (CvMoments*)malloc(sizeof(CvMoments));
	 cvMoments (imgGreenThresh, moments_green, 1);

	 double moment10_green = cvGetSpatialMoment(moments_green, 1, 0);
	 double moment01_green = cvGetSpatialMoment(moments_green, 0, 1);
	 double area_green = cvGetCentralMoment(moments_green, 0, 0);

	 static int posX_green = 0;
	 static int posY_green = 0;
	 int current_time, time_since_start;

	 stringstream green_output, green_file;
	 green_output.precision(12);

	 if(area_green!=0){
		 posX_green = moment10_green/area_green;
		 posY_green = moment01_green/area_green;

		 double lat = (((double) (posY_green)) / ((double) CAPTURE_HEIGHT) * LAT_INTERVAL) + LAT_MIN;
		 double lng = ((1 - ((double) posX_green) / ((double) CAPTURE_WIDTH)) * LNG_INTERVAL) + LNG_MIN;

		 current_time=get_time_in_msec();
		 time_since_start=current_time-start_time;
		 //printf("position of green object(%d, %d)\n", posX_green, posY_green);
		 green_file<<time_since_start <<","<< posX_green <<","<< posY_green << ";\n";
		 green_output << "{\"Username\": \"green\", \"Lat\": " << lat << ", \"Lng\": " << lng << "}";
		 RestClient::response r = RestClient::put(SERVER_URL + "/1.0/users/green", "text/json", green_output.str());
		 //cout << "Status: " << r.code << ", " << r.body;
		 cout << "G: " << lat << ", " << lng << "\n";
		 *greenfile_ptr << green_file.str();
	 }



	 return 0;
}








int main() {

	//pthread_t http_thread;
	//pthread_create(&http_thread, NULL, function1, NULL);

	 string filename_red, filename_green, filename_blue;
	 /*
	 cout << "Please provide the name of the file where you would like to save the locations for the red player \n";
	 getline (cin, filename_red);
	 cout << "Please provide the name of the file where you would like to save the locations for the green player \n";
	 getline (cin, filename_green);
	 cout << "Please provide the name of the file where you would like to save the locations for the blue player \n";
	 getline (cin, filename_blue);
	 */

	 RestClient::response r = RestClient::get("http://localhost/1.0/users/video");

	 cout << r.body;

	 filename_red = "red.txt";
	 filename_green = "green.txt";
	 filename_blue = "blue.txt";


	 time_t ltime; /* calendar time */
	 ltime=time(NULL); /* get current cal time */
	 stringstream detailed_time;
	 detailed_time << asctime( localtime(&ltime) ) << "\n";

	 ofstream myfile_red, myfile_green, myfile_blue;
	 myfile_red.open (filename_red.c_str(), ios::trunc);
	 myfile_red  << detailed_time.str();
	 myfile_green.open (filename_green.c_str(), ios::trunc);
	 myfile_green << detailed_time.str();
	 myfile_blue.open (filename_blue.c_str(),ios::trunc);
	 myfile_blue  << detailed_time.str();
	 ofstream* redfile_ptr = &myfile_red;
	 ofstream* greenfile_ptr=&myfile_green;
	 ofstream* bluefile_ptr = &myfile_blue;


	//capture is a pointer to a CvCapture structure.
	//It is initially set to 0, then we initialize it to point to the first camera on the system
	CvCapture* capture=0;
	//cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_WIDTH, );
	capture = cvCreateCameraCapture(CAM_ID);
	cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_WIDTH, CAPTURE_WIDTH);
	cvSetCaptureProperty(capture, CV_CAP_PROP_FRAME_HEIGHT, CAPTURE_HEIGHT);

	//this is to make sure that we set up the pointer capture properly. right now it points to /dev/video0
	if (!capture)
	{
		printf("Could not initialize capturing...\n");
		return -1;
	}

	if(DISPLAY_PREVIEW) {
		cvNamedWindow("video");
		cvMoveWindow("video", 0, 0);
		cvNamedWindow("thresh_red");
		cvMoveWindow("thresh_red", 720, 0);
		cvNamedWindow("thresh_blue");
		cvMoveWindow("thresh_blue", 0, 560);
		//cvNamedWindow("thresh_green");
		//cvMoveWindow("thresh_green", 720, 560);
	}

	int start_time = get_time_in_msec();
	cout<<start_time;
	while(true)
	{
		//this is where we store each image captured from the camera
		IplImage* frame=0;
		//then we request OpenCV to give us the latest frame using the cvQueryFrame function
		frame = cvQueryFrame(capture);

		//we check if the image we got was valid. If not, we break out of the loop.
		if (!frame)
			break;

		//now we show the frame
		if(DISPLAY_PREVIEW) {
			IplImage* frame_scaled = cvCreateImage(cvSize((int) frame->width * PREVIEW_SCALE, (int) frame->height * PREVIEW_SCALE), 8, 3);
			cvResize(frame, frame_scaled);
			cvShowImage("video", frame_scaled);
		}


		//now we create a delay. The program will halt for 20ms, and if there is any key pressed
		//during that time, we put that value into the variable c
		int c=cvWaitKey(20);

		//this checks if the escape key was pressed
		if((char)c==27){
			break;
			myfile_red.close();
			myfile_green.close();
			myfile_blue.close();
		}



		IplImage* imgRedThresh = GetThresholdedImageRed(frame);
		IplImage* imgBlueThresh = GetThresholdedImageBlue(frame);
		//IplImage* imgGreenThresh = GetThresholdedImageGreen(frame);
		red_calculations (imgRedThresh, redfile_ptr, start_time);
		blue_calculations (imgBlueThresh, bluefile_ptr, start_time);
		//green_calculations (imgGreenThresh, greenfile_ptr, start_time);

		if(DISPLAY_PREVIEW) {
			IplImage* imgRedThresh_scaled = cvCreateImage(cvSize((int) imgRedThresh->width * PREVIEW_SCALE, (int) imgRedThresh->height * PREVIEW_SCALE), 8, 1);
			cvResize(imgRedThresh, imgRedThresh_scaled);
			cvShowImage("thresh_red", imgRedThresh_scaled);
			IplImage* imgBlueThresh_scaled = cvCreateImage(cvSize((int) imgBlueThresh->width * PREVIEW_SCALE, (int) imgBlueThresh->height * PREVIEW_SCALE), 8, 1);
			cvResize(imgBlueThresh, imgBlueThresh_scaled);
			cvShowImage("thresh_blue", imgBlueThresh_scaled);
			//IplImage* imgGreenThresh_scaled = cvCreateImage(cvSize((int) imgGreenThresh->width * PREVIEW_SCALE, (int) imgGreenThresh->height * PREVIEW_SCALE), 8, 1);
			//cvResize(imgGreenThresh, imgGreenThresh_scaled);
			//cvShowImage("thresh_green", imgGreenThresh_scaled);

			cvReleaseImage(&imgRedThresh_scaled);
			cvReleaseImage(&imgBlueThresh_scaled);
			//cvReleaseImage(&imgGreenThresh_scaled);
		}

		cvReleaseImage(&imgRedThresh);
		cvReleaseImage(&imgBlueThresh);
		//cvReleaseImage(&imgGreenThresh);


		usleep(UPDATE_INTERVAL * 1000);

	}

	//now we release the camera for other applications
	cvReleaseCapture(&capture);
	return 0;

}


void * function1(void * argument){
    while(true){
		cout << " hello " << endl ;
		sleep(2); // fall alseep here for 2 seconds...
    }
    return 0;
}
