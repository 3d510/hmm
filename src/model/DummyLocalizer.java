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
		if (!isAdjacent(x,y,nX,nY) || isOutside(nX,nY) || isOutside(x,y))
			return 0.0;
		// same direction
		if (h == nH)
			return 0.7;
		// facing wall
		if (isOutside(x+xDir[h],y+yDir[h])) {
			if (isInCorner(x,y))
				return 1.0/2;
			return 1.0/3;
		}
		return 0.3/3;
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
	
	// Hashing scheme: state (x,y,dir) -> x*cols*4 + y*4 + dir
	public double[][] getTMatrix() {
		double[][] tMatrix = new double[rows*cols*4][rows*cols*4];
		for (int x=0; x<rows; x++) for (int y=0; y<cols; y++) for (int h=0; h<4; h++) {
			int index = x*cols*4 + y*4 + h;
			for (int dir=0; dir<4; dir++) {
				int nX = x + xDir[dir];
				int nY = y + yDir[dir];
				if (isOutside(nX,nY)) 
					continue;
				for (int nH=0; nH<4; nH++) {
					int nIndex = nX*cols*4 + nY*4 + nH;
					tMatrix[index][nIndex] = getTProb(x,y,h,nX,nY,nH);
				}
			}
		}
		return tMatrix;
	}
	
	public double[][] getEMatrix(int coord){
		/*if sensed_coord is None:
            return self.none_matrix
        width = self.width
        height = self.height*/
		double[][] matrix = new double[rows*cols*4][rows*cols*4];
		
		return matrix;
	}
	
	
	
	public List<ArrayList<Integer>> getPossibleTransitions(int row, int col, int dir) {
		List<ArrayList<Integer>> posTran = new ArrayList<ArrayList<Integer>>();
		
		return posTran;
	}
	
	public boolean isFacingWall() {
		int newX = posX + xDir[head];
		int newY = posY + yDir[head];
		return isOutside(newX, newY);
	}

	public boolean isInCorner(int x, int y) {
		if (x!=0 || x!=rows-1 || y!=0 || y!=cols-1)
			return false;
		if (x==0) 
			return y==0 || y==cols-1;
		return x == 0 || x==rows-1;
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
	
	public boolean isAdjacent(int x, int y, int nX, int nY) {
		if (x==nX)
			return y==nY-1 || y==nY+1;
		if (y==nY)
			return x==nX-1 || y==nY+1;
		return false;
	}
	
	public double[][] multiplyMatrix(double[][] matrix1, double[][] matrix2, int rows1,  int cols1, int rows2, int cols2) {
		if (cols1!=rows2)
			return null;
		double[][] result = new double[rows1][cols2];
		for (int i=0; i<rows1; i++) for (int j=0; j<cols2; j++) {
			double ans = 0.0;
			for (int k=0; k<cols1; k++) 
				ans += matrix1[i][k] * matrix2[k][j];
			result[i][j] = ans;
		}
		return result;
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