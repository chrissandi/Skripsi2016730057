
public class PCA {
	private double[][] data;
	private double[] stdDev;
	private double[] rata;
	private double[][] stzData;
    
    public PCA(double[][] data) {
    	this.data = data;
    	this.stdDev = new double[data[0].length];
    	this.rata = new double[data[0].length];
    	this.stzData = new double[data.length][data[0].length];
    }
    public void standarizeData(){
    	 double temp = 0;
         for(int i=0;i<data[0].length;i++){
             for(int j=0;j<data.length;j++){
                 temp+=data[j][i];
             }
             rata[i]=temp/data.length;
             temp = 0;
         }
         for(int i=0;i<data[0].length;i++){
             for(int j=0;j<data.length;j++){
                 temp+=Math.pow(data[j][i]-rata[i],2);
             }
             stdDev[i]=Math.sqrt(temp/(data.length-1));
             temp = 0;
         }
         for(int i=0;i<data[0].length;i++){
             for(int j=0;j<data.length;j++){
                 stzData[j][i] = (data[j][i]-rata[i])/stdDev[i];
             }
         }
    }
    public double mean(double[] arr, int n){
        double sum = 0;
        for(int i = 0; i < n; i++)
            sum = sum + arr[i];
        return sum / n;
    }
    
    public double[] getColumn(double[][] arr,int j){
        double[] res = new double[arr.length];
        for(int i=0;i<arr.length;i++){
            res[i]= arr[i][j];
        }
        return res;
    }
    
    public double covariance(double[] arr1, double[] arr2){
        int n = arr1.length;
        double sum = 0;
        for(int i = 0; i < n; i++)
            sum = sum + (arr1[i] - mean(arr1, n))*(arr2[i] - mean(arr2, n));
        return sum / (n - 1);
    }
    
    public double[][] getCovMatrices(){
        double[][] cov = new double[stzData[0].length][stzData[0].length];
        for(int i=0;i<cov.length;i++){
            for(int j=0;j<cov.length;j++){
                cov[i][j] = covariance(getColumn(stzData,i),getColumn(stzData,j));
            }
        }
        return cov;
    }
    public double[] getPC(){
    	standarizeData();
        EigenvalueDecomposition e = new EigenvalueDecomposition(new Matrix(getCovMatrices()));
        double[] vec = getColumn(e.getV().getArray(),e.getV().getArray().length-1);
        double[] pc = new double[stzData.length];
        double temp = 0;
        for(int i=0;i<pc.length;i++){
            for(int j=0;j<stzData[0].length;j++){
                temp+= stzData[i][j]*vec[j];
            }
            pc[i] = temp;
            temp = 0;
        }
        return pc;
    }
}

