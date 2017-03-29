package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import control.EstimatorInterface;

public class DummyLocalizer implements EstimatorInterface {
		
	private int rows, cols, head, curHead, posX, posY;
	private int[] sensor = new int[2];
	Random rand = new Random();
	int[] xDir = {-1,0,1,0}; // UP,RIGHT,DOWN,LEFT
	int[] yDir = {0,1,0,-1};
	double[][] fMatrix,tMatrix;

	public DummyLocalizer( int rows, int cols, int head) {
		this.rows = rows;
		this.cols = cols;
		this.head = head;		
		this.curHead = rand.nextInt(head);
		this.posX = rand.nextInt(rows);
		this.posY = rand.nextInt(cols);
		this.tMatrix = getTMatrix();
		this.fMatrix = initializeFMatrix(); 
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
		if (h == nH && nX == x+xDir[h] && nY == y+yDir[h])
			return 0.7;
		if (h == nH)
			return 0.0;
		// different direction
		double total_probs = 0.3;
		if (isFacingWall(x,y,h))
			total_probs = 1.0;
		int count = 0;
		for (int i=0; i<4; i++) 
			if (i!=h && !isOutside(x+xDir[i],y+yDir[i]))
				count++;
		for (int i=0; i<4; i++) 
			if (i==nH && nX==x+xDir[i] && nY==y+yDir[i])
				return total_probs/count;
		return 0.0;
	}

	public double getOrXY( int rX, int rY, int x, int y) {
		List<ArrayList<Integer>> firstField = possibleLoc1(x,y);
		for (ArrayList<Integer> pos: firstField) {
			if (rX == pos.get(0) && rY == pos.get(1))
				return 0.05;
		}
		List<ArrayList<Integer>> secondField = possibleLoc2(x,y);
		for (ArrayList<Integer> pos: secondField) {
			if (rX == pos.get(0) && rY == pos.get(1))
				return 0.025;
		}
		if (x==rX && y==rY)
			return 0.1;
		else if(rX==-1 ||rY==-1)
			return 1 - 0.1 - 0.05*firstField.size() - 0.025*secondField.size();
		return 0.0;
	}

	public int[] getCurrentTruePosition() {
		int[] ret = new int[2];
		ret[0] = posX;
		ret[1] = posY;
		return ret;
	}
	
	public void moveRobot() {
		int[] ret = new int[2];
		
		if(isFacingWall(posX,posY,curHead)){
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
				ret[0] = posX + xDir[curHead];
				ret[1] = posY + yDir[curHead];
			}
		}
		posX = ret[0];
		posY = ret[1];
	}

	public int[] getCurrentReading() {
		return sensor;
	}
	
	public void readSensor() {
		int[] ret = new int[2];
		double prob = rand.nextDouble();
		
		List<ArrayList<Integer> > firstField = possibleLoc1(posX,posY);
		List<ArrayList<Integer> > secondField = possibleLoc2(posX,posY);
		
		if(prob <= 0.1){
			ret[0] = posX;
			ret[1] = posY;
		}else if(prob <= 0.1 + firstField.size() * 0.05){
			ret = getRandomLoc1();
		}else if(prob <= 0.1 + firstField.size() * 0.05 + secondField.size() * 0.025)
			ret = getRandomLoc2();
		else {
			ret[0]=-1;
			ret[1]=-1;
		}
		sensor = ret;
	}


	public double getCurrentProb( int x, int y) {
		double ret = 0.0;
		for (int i=0; i<4; i++) {
			ret += fMatrix[x*cols*4+y*4+i][0];
		}
		return ret;
	}
	
	public void update() {
		moveRobot();
		readSensor();
		double[][] oMatrix = getOMatrix(sensor);
		//double[][] tMatrix = getTMatrix();
		//forward step
//		double[][]temp = multiplyMatrix(oMatrix,tranMatrix(tMatrix,rows*cols*4,rows*cols*4),rows*cols*4,rows*cols*4,rows*cols*4,rows*cols*4);
//		fMatrix = multiplyMatrix(temp,fMatrix,rows*cols*4,rows*cols*4,rows*cols*4,1);
		
		double[][] newFMatrix = new double[rows*cols*4][1];
		for (int i=0; i<rows*cols*4; i++) {
			newFMatrix[i][0]=0;
			for (int j=0; j<rows*cols*4; j++)
				newFMatrix[i][0] += oMatrix[i][i]*tMatrix[j][i]*fMatrix[j][0];
		}
		
		for (int i=0; i<rows*cols*4; i++) 
			fMatrix[i][0] = newFMatrix[i][0];
		
		double sum=0.0;
		for (int i=0; i<rows*cols*4; i++)
			sum += fMatrix[i][0];
		if (sum==0) 
			return;
		for (int i=0; i<rows*cols*4; i++)
			fMatrix[i][0]/=sum;
		
	}
	
	public void print(double[][] arr, int size1, int size2, boolean isDiag) {
		if (isDiag) {
			for (int i=0; i<size1; i++) 
				System.out.printf("%.4f ",arr[i][i]);
			System.out.println();
			return;
		} 
		for (int i=0; i<size1; i++) {
			for (int j=0; j<size2; j++)
				System.out.printf("%.4f ", arr[i][j]);
			System.out.println();
		}
	}
	
	public int[] getRandomLoc1(){
		int [] ret = {-1,-1};
		List<ArrayList<Integer> > firstField = possibleLoc1(posX,posY);
//		List<ArrayList<Integer> > firstField = new ArrayList<ArrayList<Integer>>();
//		
//		int[] xDiff = {-1,-1,-1,0,0,1,1,1};
//		int[] yDiff = {-1,0,1,-1,1,-1,0,1};
//		
//		for (int i=0; i<8; i++) {
//			ArrayList<Integer> adjPos = new ArrayList<Integer>();
//			adjPos.add(posX+xDiff[i]);
//			adjPos.add(posY+yDiff[i]);
//			firstField.add(adjPos);
//		}
		
		int randIndex = rand.nextInt(firstField.size());
		
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
		
//		List<ArrayList<Integer> > secondField = new ArrayList<ArrayList<Integer>>();
//		
//		int[] xDiff = {-2,-2,-2,-2,-2,-1,-1,0,0,1,1,2,2,2,2,2};
//		int[] yDiff = {-2,-1,0,1,2,-2,2,-2,2,-2,2,-2,-1,0,1,2};
//		
//		for (int i=0; i<16; i++) {
//			ArrayList<Integer> adjPos = new ArrayList<Integer>();
//			adjPos.add(posX+xDiff[i]);
//			adjPos.add(posY+yDiff[i]);
//			secondField.add(adjPos);
//		}
		
		List<ArrayList<Integer> > secondField = possibleLoc2(posX,posY);
		
		int randIndex = rand.nextInt(secondField.size());
		
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
			for (int nx=0; nx<rows; nx++) for (int ny=0; ny<cols; ny++) for (int nh=0; nh<4; nh++) {
				int nindex = nx*cols*4 + ny*4 + nh;
				tMatrix[index][nindex] = getTProb(x,y,h,nx,ny,nh);
			}
		}
		return tMatrix;
	}
	
	public double[][] getOMatrix(int[] coord) {
//		if(coord == null)
//            return getNullMatrix();
//		
//		int x= coord[0];
//		int y= coord[1];
//		double[][] matrix = new double[rows*cols*4][rows*cols*4];
//		
//		int index = x * cols * 4 + y * 4;
//		
//        for(int i = 0; i < 4 ; i++){
//        	matrix[index + i][index + i] = 0.1;
//        }
//        setProbNeighbor(matrix, possibleLoc1(x, y), 0.05);
//        setProbNeighbor(matrix, possibleLoc2(x, y), 0.025);
//		return matrix;
		
		double[][] oMatrix = new double[rows*cols*4][rows*cols*4];
		
		for (int x=0; x<rows; x++) for (int y=0; y<cols; y++) {
			double prob = getOrXY(coord[0],coord[1],x,y);
			int index = x * cols * 4 + y * 4;
	        for(int i = 0; i < 4 ; i++){
	        	oMatrix[index + i][index + i] = prob;
	        }
		}
		return oMatrix;
	}
	
	public double[][] getNullMatrix(){
		double[][] matrix = new double[rows*cols*4][rows*cols*4];
		
		for(int i = 0;i < rows*cols*4; i++) for(int j = 0; j < rows*cols*4; j++){
			matrix[i][j] = 0;
		}
		
		for(int i = 0; i < cols * rows * 4; i++){
            int posX = i / (cols * 4);
            int posY = (i / 4) % cols;

            int prob1 = possibleLoc1(posX, posY).size();
            int prob2 = possibleLoc2(posX, posY).size();
            
            matrix[i][i] = 1 - (0.1 + prob1 * 0.05 + prob2 * 0.025);
		}
		
		return matrix;
	}

	public void setProbNeighbor(double[][] matrix, List<ArrayList<Integer> > possibleLoc2, double prob){
		for(ArrayList<Integer> element: possibleLoc2){
			int ind = element.get(0) * cols  * 4 + element.get(1) * 4;
			for(int i = 0; i < 4; i++){
				matrix[ind + i][ind + i] = prob;
			}
		}
	}	
	
	public List<ArrayList<Integer> > possibleLoc1(int posX, int posY){
		List<ArrayList<Integer> > firstField = new ArrayList<ArrayList<Integer>>();
		
		int[] xDiff = {-1,-1,-1,0,0,1,1,1};
		int[] yDiff = {-1,0,1,-1,1,-1,0,1};
		
		for (int i=0; i<8; i++) {
			ArrayList<Integer> adjPos = new ArrayList<Integer>();
			int xSelected = posX+xDiff[i];
			int ySelected = posY+yDiff[i];
			adjPos.add(xSelected);
			adjPos.add(ySelected);
			
			if(!isOutside(xSelected,ySelected))
				firstField.add(adjPos);
				
		}   
        return firstField;    
		
	}
	
	public List<ArrayList<Integer> > possibleLoc2(int posX, int posY){
		List<ArrayList<Integer> > secondField = new ArrayList<ArrayList<Integer>>();
		
		int[] xDiff = {-2,-2,-2,-2,-2,-1,-1,0,0,1,1,2,2,2,2,2};
		int[] yDiff = {-2,-1,0,1,2,-2,2,-2,2,-2,2,-2,-1,0,1,2};
		
		for (int i=0; i<16; i++) {
			ArrayList<Integer> adjPos = new ArrayList<Integer>();
			int xSelected = posX+xDiff[i];
			int ySelected = posY+yDiff[i];
			adjPos.add(xSelected);
			adjPos.add(ySelected);
			
			if(!isOutside(xSelected,ySelected))
				secondField.add(adjPos);
		}
		
		return secondField;
	}

	public boolean isFacingWall(int x,int y, int h) {
		int newX = x + xDir[h];
		int newY = y + yDir[h];
		return isOutside(newX, newY);
	}

	public boolean isInCorner(int x, int y) {
		if (x!=0 || x!=rows-1 || y!=0 || y!=cols-1)
			return false;
		if (x==0) 
			return y==0 || y==cols-1;
		return x == 0 || x==rows-1;
	}
	
	public boolean isOutside(int xCoord, int yCoord) {
		return xCoord < 0 || xCoord >= rows || yCoord < 0 || yCoord >= cols;
	}
	
	public boolean isAdjacent(int x, int y, int nX, int nY) {
		if (x==nX)
			return y==nY-1 || y==nY+1;
		if (y==nY)
			return x==nX-1 || x==nX+1;
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
	
	public double[][] tranMatrix(double[][] matrix, int row, int col) {
		double[][] ret = new double[col][row];
		for (int i=0; i<row; i++) for (int j=0; j<col;j++) {
			ret[j][i] = matrix[i][j];
		}
		return ret;
	}
	
	public int randDirection(){
		ArrayList<Integer> possibleDir = new ArrayList<Integer>();
		for (int i=0; i<4; i++)
			if (i!=curHead && !isOutside(posX+xDir[i],posY+yDir[i]))
				possibleDir.add(i);
		int randIndex = rand.nextInt(possibleDir.size());
		return possibleDir.get(randIndex);
	}
	
	public double[][] initializeFMatrix(){
		double[][] fMatrix = new double[rows*cols*4][1];
		
		for(int i = 0;i < rows*cols*4; i++){
			fMatrix[i][0] = 1.0 / (rows*cols*4);
		}
		return fMatrix;
	}
	
}