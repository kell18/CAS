[X, y] = prepareClassData('data-train_11-06.txt');
[tX, ty] = prepareClassData('data-test_11-06.txt');
[vX, vy] = prepareClassData('data-validation_11-06.txt');

ch = X(:, 2:end);
opts = optimset('GradObj', 'on', 'MaxIter', '10000');

d = 0;
maxD = 30;
rndIters = 15;

minVal = 1500.0;
rFP = 100;
rFN = 100;
thR = [];
info = 10;
dR = 1;
jR = -1;
errHist = [];

while (d < maxD)
  d += 1;
  dX = lowerDegree(X, d);
  costFn = @(th) costFunction(th, dX, y);
  sz2 = 1 + size(ch)(2) * d;
  for i = 1:rndIters
    th0 = (rand(sz2, 1) .- 0.5) .* 10;
    [th, fV, inf, out, grad, hess] = fminunc(costFn, th0, opts);
    [j, g, FP, FN] = costFunction(th, lowerDegree(vX, d), vy);
    if (FP + FN < minVal)
      minVal = FP + FN;
      rFP = FP;
      rFN = FN;
      thR = th;
      info = inf;
      dR = d;
      jR = j;
    end
  end
  errHist = [errHist; d, FP, FN];
end

disp('Validation min: ')
rFP
rFN
thR
info
dR
jR
errHist

disp('Testing: ')
[tj, tg, tFP, tFN] = costFunction(thR, lowerDegree(tX, dR), ty);
tj
tFP
tFN

%mX = linspace(0, 1.8, 30);
%mY = linspace(0, 1.0, 30);
%[XX, YY] = meshgrid(mX, mY);
%ZZ = th(1)/th(2) .+ th(3)/th(2) .* XX .+ th(4)/th(2) .* YY;  