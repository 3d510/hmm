package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import control.EstimatorInterface;

public class DummyLocalizer implements EstimatorInterface {
		
	private int rows, cols, head, posX, posY;
	Random rand = new Random();
	int[] xDir = {-1,0,1,0}; // UP,RIGHT,DOWN,LEFT
	int[] yDir = {0,1,0,-1};

	public DummyLocalizer( int rows, int cols, int head) {
		this.rows = rows;
		this.cols = cols;
		this.head = head;		
		this.posX = rand.nextInt(rows);
		this.posY = rand.nextInt(cols);
	}	
	
	public int getNumRows() {
		return rows;
	}
	
	public int getNumCols() {
		return cols;
	}
	
	public int getNumHead() {
		return head;
	}
	
	public double getTProb( int x, int y, int h, int nX, int nY, int nH) {
		return 0.0;
	}

	public double getOrXY( int rX, int rY, int x, int y) {
		return 0.1;
	}


	public int[] getCurrentTruePosition() {
		int[] ret = new int[2];
		
		if(isFacingWall()){
			int dirIndex = randDirection(); 
			ret[0] = posX + xDir[dirIndex];
			ret[1] = posY + yDir[dirIndex];
		} else {
			double prob = rand.nextDouble();
			if (prob<=0.3) {
				int dirIndex = randDirection(); 
				ret[0] = posX + xDir[dirIndex];
				ret[1] = posY + yDir[dirIndex];
			} else {
				ret[0] = posX + xDir[head];
				ret[1] = posY + yDir[head];
			}
		}
		posX = ret[0];
		posY = ret[1];
		return ret;
	}

	public int[] getCurrentReading() {
		int[] ret = new int[2];
		double prob = rand.nextDouble();
		
		if(prob <= 0.1){
			ret[0] = posX;
			ret[1] = posY;
		}else if(prob <= 0.1 + 8 * 0.5){
			ret = getRandomLoc1();
		}else if(prob <= 0.1 + 8 * 0.5 + 16 * 0.025)
			ret = getRandomLoc2();
		else
			ret = null;
		return ret;
	}


	public double getCurrentProb( int x, int y) {
		double ret = 0.0;
		return ret;
	}
	
	public void update() {
		System.out.println("Nothing is happening, no model to go for...");
	}
	
	public int[] getRandomLoc1(){
		int [] ret = {-1,-1};
		List<ArrayList<Integer> > firstField = new ArrayList<ArrayList<Integer>>();
		
		int[] xDiff = {-1,-1,-1,0,0,1,1,1};
		int[] yDiff = {-1,0,1,-1,1,-1,0,1};
		
		for (int i=0; i<8; i++) {
			ArrayList<Integer> adjPos = new ArrayList<Integer>();
			adjPos.add(posX+xDiff[i]);
			adjPos.add(posY+yDiff[i]);
			firstField.add(adjPos);
		}
		
		int randIndex = rand.nextInt(8);
		
		ArrayList<Integer> selected = firstField.get(randIndex);
		int xSelected = selected.get(0);
		int ySelected = selected.get(1);
		
		if(isOutside(xSelected,ySelected))
			return null;
		
		ret[0] = xSelected;
		ret[1] = ySelected;
		
		return ret;
	}
	
	public int[] getRandomLoc2(){
		int [] ret = {-1,-1};
		List<ArrayList<Integer> > secondField = new ArrayList<ArrayList<Integer>>();
		
		int[] xDiff = {-2,-2,-2,-2,-2,-1,-1,0,0,1,1,2,2,2,2,2};
		int[] yDiff = {-2,-1,0,1,2,-2,2,-2,2,-2,2,-2,-1,0,1,2};
		
		for (int i=0; i<16; i++) {
			ArrayList<Integer> adjPos = new ArrayList<Integer>();
			adjPos.add(posX+xDiff[i]);
			adjPos.add(posY+yDiff[i]);
			secondField.add(adjPos);
		}
		
		int randIndex = rand.nextInt(16);
		
		ArrayList<Integer> selected = secondField.get(randIndex);
		int xSelected = selected.get(0);
		int ySelected = selected.get(1);
		
		if(isOutside(xSelected,ySelected))
			return null;
		
		ret[0] = xSelected;
		ret[1] = ySelected;
		
		return ret;
	}
	
	public double[][] getTMatrix() {
		double[][] tMatrix = new double[rows*cols*4][rows*cols*4];
		for (int index=0; i<rows*cols*4; i++) {
			int row = ;
			int col = ;
			int dir = i%(4);
		}
		return tMatrix;
	}
	
	
	
	public boolean isFacingWall() {
		int newX = posX + xDir[head];
		int newY = posY + yDir[head];
		return isOutside(newX, newY);
	}
	
//	public boolean isInCorner() {
//		if (posX!=0 || posX!=rows-1 || posY!=0 || posY!=cols-1)
//			return false;
//		if (posX==0) 
//			return posY==0 || posY==cols-1;
//		return posX == 0 || posX==rows-1;
//	}
	
	public boolean isOutside(int xCoord, int yCoord) {
		return xCoord < 0 || xCoord >= rows || yCoord < 0 || yCoord >= cols;
	}
	
	public int randDirection(){
		ArrayList<Integer> possibleDir = new ArrayList<Integer>();
		for (int i=0; i<4; i++)
			if (i!=head && !isOutside(posX+xDir[i],posY+yDir[i]))
				possibleDir.add(i);
		int randIndex = rand.nextInt(possibleDir.size());
		return possibleDir.get(randIndex);
	}
	
}