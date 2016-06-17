%% Pick degree and coefs for log regression polynom

[X, y] = prepareClassData('data/data-train_oct.txt');
[tX, ty] = prepareClassData('data/data-test_oct.txt');
[vX, vy] = prepareClassData('data/data-validation_oct.txt');

% X(:, 4:end) = tX(:, 4:end) = vX(:, 4:end) = [];

opts = optimset('GradObj', 'on', 'MaxIter', '100');

d = 0; maxD = 10; rndIters = 20;

minVal = 1500.0;
rFP = -1; rFN = -1; rTP = -1; rTN = -1;
thR = [];
info = 10;
dR = 1;
jR = -1;
errHist = [];

while (d < maxD)
  d += 1;
  dX = raiseDegree(X, d);
  costFn = @(th) costFunction(th, dX, y);
  sz2 = size(X)(2) * d;
  for i = 1:rndIters
    th0 = (rand(sz2, 1) .- 0.5) .* 10;
    [th, fV, inf, out, grad, hess] = fminunc(costFn, th0, opts);
    [j, g, FP, FN, TP, TN] = costFunction(th, raiseDegree(vX, d), vy);
    if (FP + FN < minVal)
      minVal = FP + FN;
      rFP = FP; rFN = FN; rTP = TP; rTN = TN;
      thR = th;
      info = inf;
      dR = d;
      jR = j;
    end
  end
  errHist = [errHist; d, FP, FN];
end

disp('Train: ')
[tj, tg, tFP, tFN, tTP, tTN] = costFunction(thR, raiseDegree(X, dR), y);
tFP, tFN, tTP, tTN
tj

disp('Validation: ')
rFP, rFN, rTP, rTN, thR, info, dR, jR
% errHist

disp('Testing: ')
[tj, tg, rFP, rFN, rTP, rTN] = costFunction(thR, raiseDegree(tX, dR), ty);
rFP, rFN, rTP, rTN
Accur = (rTP + rTN) / (rFP + rFN + rTP + rTN)
Precision = rTN / (rTN + rFP)
Recall = rTN / (rTN + rFN)
tj
