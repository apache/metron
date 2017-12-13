package org.apache.metron.statistics.outlier.rpca;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class RidgeRegression {

  private RealMatrix X;
  private SingularValueDecomposition X_svd = null;
  private double[] Y;
  private double l2penalty;
  private double[] coefficients;
  private double[] standarderrors;

  private double[] fitted;
  private double[] residuals;

  public RidgeRegression(double[][] x, double[] y) {
    this.X = MatrixUtils.createRealMatrix(x);
    this.X_svd = null;
    this.Y = y;
    this.l2penalty = 0;
    this.coefficients = null;

    this.fitted = new double[y.length];
    this.residuals = new double[y.length];
  }

  public void updateCoefficients(double l2penalty) {
    if (this.X_svd == null) {
      this.X_svd = new SingularValueDecomposition(X);
    }
    RealMatrix V = this.X_svd.getV();
    double[] s = this.X_svd.getSingularValues();
    RealMatrix U = this.X_svd.getU();

    for (int i = 0; i < s.length; i++) {
      s[i] = s[i] / (s[i]*s[i] + l2penalty);
    }
    RealMatrix S = MatrixUtils.createRealDiagonalMatrix(s);

    RealMatrix Z = V.multiply(S).multiply(U.transpose());

    this.coefficients = Z.operate(this.Y);

    this.fitted = this.X.operate(this.coefficients);
    double errorVariance = 0;
    for (int i = 0; i < residuals.length; i++) {
      this.residuals[i] = this.Y[i] - this.fitted[i];
      errorVariance += this.residuals[i] * this.residuals[i];
    }
    errorVariance = errorVariance / (X.getRowDimension() - X.getColumnDimension());

    RealMatrix errorVarianceMatrix = MatrixUtils.createRealIdentityMatrix(this.Y.length).scalarMultiply(errorVariance);
    RealMatrix coefficientsCovarianceMatrix = Z.multiply(errorVarianceMatrix).multiply(Z.transpose());
    this.standarderrors = getDiagonal(coefficientsCovarianceMatrix);
  }

  private double[] getDiagonal(RealMatrix X) {
    double[] diag = new double[X.getColumnDimension()];
    for (int i = 0; i < diag.length; i++) {
      diag[i] = X.getEntry(i, i);
    }
    return diag;
  }

  public double getL2penalty() {
    return l2penalty;
  }

  public void setL2penalty(double l2penalty) {
    this.l2penalty = l2penalty;
  }

  public double[] getCoefficients() {
    return coefficients;
  }

  public double[] getStandarderrors() {
    return standarderrors;
  }
}
