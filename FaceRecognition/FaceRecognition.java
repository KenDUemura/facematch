import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.googlecode.javacv.cpp.opencv_objdetect;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_objdetect.cvHaarDetectObjects;

public class FaceRecognition {
	// The cascade definition to be used for detection.
	private static  String CASCADE_FILE ;
	private static int index;
	public String[] faceCrop(String cascade_file, String imgpath){
		CASCADE_FILE = cascade_file;
		opencv_core.IplImage originalImage = opencv_highgui.cvLoadImage(
				imgpath, 1);

		// We need a grayscale image in order to do the recognition, so we
		// create a new image of the same size as the original one.
		opencv_core.IplImage grayImage = opencv_core.IplImage.create(
				originalImage.width(), originalImage.height(),
				opencv_core.IPL_DEPTH_8U, 1);

		// We convert the original image to grayscale.
		cvCvtColor(originalImage, grayImage, CV_BGR2GRAY);

		opencv_core.CvMemStorage storage = opencv_core.CvMemStorage.create();

		// We instantiate a classifier cascade to be used for detection,
		// using the cascade definition.
		opencv_objdetect.CvHaarClassifierCascade cascade = new opencv_objdetect.CvHaarClassifierCascade(
				cvLoad(CASCADE_FILE));

		// We detect the faces.
		opencv_core.CvSeq faces = cvHaarDetectObjects(grayImage, cascade,
				storage, 1.1, 1, 0);

		// We iterate over the discovered faces and draw yellow rectangles
		// around them.
		String imgpaths[] = new String[faces.total()];
		for (int i = 0; i < faces.total(); i++) {
			opencv_core.CvRect r = new opencv_core.CvRect(
					cvGetSeqElem(faces, i));
			cvRectangle(originalImage, cvPoint(r.x(), r.y()),
					cvPoint(r.x() + r.width(), r.y() + r.height()),
					opencv_core.CvScalar.GREEN, 1, CV_AA, 0);
			//AddInfo.addinfo(r.x(), r.y());
			imgpaths[i] = myCreateImage(r.x(), r.y(), r.width(), r.height(),imgpath);
			
		}

		// Save the image to a new file.
		//cvSaveImage("images/output.jpg", originalImage);
		return imgpaths;
	}
	public  String myCreateImage(int x,int y,int w,int h,String imgpath) {  
	    int width = 100;  // or, = image.getWidth(this);  
	    int height = 100;  
	    // Create a buffered image in which to draw  
	    BufferedImage bi = new BufferedImage(width, height,  
	                                          BufferedImage.TYPE_INT_RGB);  
	    BufferedImage fixedbi = new BufferedImage(width, height,  
                BufferedImage.TYPE_INT_RGB);
	    // Draw image into bufferedImage.  
	    String[] name = imgpath.split("_original\\.");
	    Graphics2D g = bi.createGraphics();  
	    //Output files path, for temp use
	    File of = new File(name[0]+index+"_crop"+".jpg");
	   
	    try {
	    	//Input image.
	    	bi = ImageIO.read(new File(imgpath));
	    	//get subimage, quite simple.
	    	BufferedImage subbi = bi.getSubimage(x, y, w, h);
	    	System.out.println("index:"+index);
	    	g = fixedbi.createGraphics();
	    	g.drawImage(subbi,0,0,100,100,null);
	    	g.dispose();
			ImageIO.write(fixedbi,"jpg",of);
			index++;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	    System.out.println(of.getPath());
	    return of.getPath();  
	}  
	public static void main(String[] args) throws Exception {
		// Load the original image.
		FaceRecognition fr = new FaceRecognition();
		String cf = args[0];//"F:\\opencv\\opencv\\data\\haarcascades\\haarcascade_frontalface_alt.xml";//
		String imgpath = args[1];// "E:\\images";//
		File imgFolder =new File(imgpath);
		File[] getAllImgs = imgFolder.listFiles();
		String imgpaths[];
		for(int i=0; i<getAllImgs.length;i++){
			index = 0;
			imgpath = imgpath+java.io.File.separator;
			imgpaths= fr.faceCrop(cf, imgpath+getAllImgs[i].getName());
		}
		for(int i = 0;i<getAllImgs.length;i++){
			
			System.out.println(getAllImgs[i].getName());
		}
	}
}
