package application;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

/**
 * 
 * @author James Richardson
 * 
 * References: Class notes, Stack Overflow
 * Assistance by: Andrew Brennan, Austin Heraughty, Dylan Richards, Gianluca Zuccarelli
 *
 */

public class MainController {

	@FXML
	ImageView imageview;

	@FXML
	ListView listview;

	@FXML
	TextArea textarea;
	
	@FXML
	Label intLabel;
	
	@FXML
	Pane pane;


	Image origImage;
	WritableImage BandWImage;
	WritableImage boxImage;
	File file;
	int[] DJset;
	int[] sets;
	int dsindex;
	int width;
	int birdId;
	private Set<Integer> birds = new HashSet<>();
	private Rectangle[] birdBoxes;
	private Rectangle outerRec;
	

	public void regularImage() {// go back to the original image
		imageview.setImage(origImage);
	}

	public void blackAndWhiteImage() {// using the blackAndWhite method below, changes original image to black and
										// white as well as unions all adjacent black pixels to birds
		blackAndWhite();//sets the image to black and white
		imageview.setImage(BandWImage);
		setPixelColorValue();// sets the value to either white or black
	}
	
	public void identifyBirds() {
		unionTheAdjacentPixels();// unions all adjacent black pixels to sets or "birds"
	}

	public void countTheBirds() {// Displays the "bird count"
		noiseManagement();
		intLabel.setText("Bird Count: " + birds.size() );
	}

	public void chooseImage() {//select a new image from the dialog box
		FileChooser fc = new FileChooser();
		fc.setTitle("Choose A File");
		file = fc.showOpenDialog(imageview.getScene().getWindow());// brings up the dialog window to choose a new image

		if (file != null) {
			origImage = new Image(file.toURI().toString());// Original Image, no changes
			imageview.setImage(origImage);
		} else {
			System.out.println("File is not valid");

		}
	}

	public void blackAndWhite() {//converts the original image to black and white
		PixelReader pixelReader = origImage.getPixelReader();
		int width = (int) origImage.getWidth();
		int height = (int) origImage.getHeight();
		BandWImage = new WritableImage(width, height);
		PixelWriter pixelWriter = BandWImage.getPixelWriter();
		for (int y = 0; y < height; y++) {// iterate through the image pixel by pixel
			for (int x = 0; x < width; x++) {
				Color color = pixelReader.getColor(x, y);
				pixelWriter.setColor(x, y,
						(color.getRed() + color.getGreen() + color.getBlue() >= 2.2) ? Color.WHITE : Color.BLACK);
			}
		}
	}

	public void setPixelColorValue() {// iterate through the black and white image and set white to -1, and black to
										// its own value
		PixelReader pixelReader = BandWImage.getPixelReader();
		int width = (int) BandWImage.getWidth();
		int height = (int) BandWImage.getHeight();
		DJset = new int[width * height];// new 1D int array, "disjoint set"
		for (int i = 0; i < DJset.length; i++) {
			DJset[i] = i;// the array holds a reference to all the pixels in the image
		}
		try {
		for (int y = 0; y < height; y++) {// iterate through the pixels
			for (int x = 0; x < width; x++) {
				dsindex = (y * width) + x;// convert x and y coordinates into a single value
				if (pixelReader.getColor(x, y).equals(Color.WHITE)) {// if white
					DJset[dsindex] = -1;// set to -1
				}
				if (pixelReader.getColor(x, y).equals(Color.BLACK)) {// if black
					DJset[dsindex] = dsindex;// set the black pixel's value to itself
				}
			}
		} 
			
		}catch (ArrayIndexOutOfBoundsException boundsException) {
			
		}
	}

	public void unionTheAdjacentPixels() {// iterate through the 1D array, union all adjacent black pixels to create
											// sets
		int width = (int) BandWImage.getWidth();
		int height = (int) BandWImage.getHeight();
		try {
			for (int y = 0; y < height; y++) {// iterate through the pixels
				for (int x = 0; x < width; x++) {
				int dsindex = (y * width) + x;// convert x and y coordinates into a single value
					if (DJset[dsindex] != -1) {// if pixel isn't white
						if ((DJset[dsindex + 1]%width != 0)&&(DJset[dsindex + 1] != -1)) {// if the next index doesnt wrap around, and isn't white																	
							union(DJset, DJset[dsindex], DJset[dsindex + 1]);// union with dsindex
						}
						if ((DJset[dsindex + width]<DJset.length-width)&&(DJset[dsindex + width] != -1)) {// if the pixel isn't below the last line, and isn't white																
							union(DJset, DJset[dsindex], DJset[dsindex + width]);// union with dsindex
						}
					} 
				}
			}
		}catch (ArrayIndexOutOfBoundsException boundsException) {
	}
		addingRootsToBirdsSet();
		noiseManagement();
		createBirdBoxes();

	}

	private void addingRootsToBirdsSet() {//adds all roots to the Set "birds"
		for (int id = 0; id < DJset.length; id++) {
			int parent = find(DJset, id);
			if (parent != -1)
				birds.add(parent);
		}
	}

	private int blackPixelsPerBird(int parent) {//returns a count of all the black pixels per "bird"
		int count = 0;
		for (int i = 0; i < DJset.length; i++) {
			if (parent == find(DJset, i)) {
				count++;
			}
		}
		return count;
		
	}
	
	private void noiseManagement() {// Remove small sets
		//remove if the percentage of pixels in the set is less than 5% of the overall image size
		birds.removeIf(b->((double) blackPixelsPerBird((int) b) / DJset.length) * 100.0 < 0.05);
	}
	
	public void displayDSArray() {//Displays the converted pixels into the console
		int width = (int) BandWImage.getWidth();
		for (int i = 0; i < DJset.length; i++) {
			System.out.print(DJset[i]+" ");
			if(i % width == 0) {
				System.out.println();
			}
		}	
	}
	
	private void printRoot() {//console print out of all the black pixels and their respective roots
		for (int id = 0; id < DJset.length; id++) {
			int parent = find(DJset, id);
			if (parent != -1)
				System.out.println("The root of " + id + " is " + parent);
		}
	}

	private void union(int[] a, int p, int q) {
		a[find(a, q)] = find(a, p);
	}

	private int find(int[] a, int id) {
		if (a[id] == -1)
			return -1;

		while (a[id] != id)
			id = a[id];
		return id;
	}
	
	public Rectangle[] getBirdBoxes() {
		return birdBoxes;
	}
	
	public Set<Integer> getBirds() {
		return birds;
	}
	
	private void createBirdBoxes() {
		birdBoxes = new Rectangle[birds.size()];
		int width = (int) BandWImage.getWidth();
		int index = 0;
		for (Iterator<Integer> iterator = birds.iterator(); iterator.hasNext();) {
			
			Integer element = iterator.next();
			
			int x = element % width;
			int y = element / width;

			int minX = x, maxX = x, minY = y, maxY = y;
			for (int i = 0; i < DJset.length; i++) {
				if (DJset[i] == -1)//if white
					continue;

				if (element == find(DJset, i)) {
					
					int xLoc = i % width;
					int yLoc = i / width;

					minX = (minX > xLoc) ? xLoc : minX;
					maxX = (maxX < xLoc) ? xLoc : maxX;

					minY = (minY > yLoc) ? yLoc : minY;
					maxY = (maxY < yLoc) ? yLoc : maxY;
				}
			}	
			birdBoxes[index++] = new Rectangle(minX, minY, maxX - minX, maxY - minY);
			pane.getChildren().addAll(birdBoxes[index++]);
		}	
	}

	public void exitSystem() {// exits the system
		System.exit(0);
	}
}
