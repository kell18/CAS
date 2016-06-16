function [J, grad, FP, FN, TP, TN] = costFunction(theta, X, y)

m = length(y);
J = 0;
grad = zeros(size(theta));

identity = ones(m, 1);
hX = sigmoid(X * theta);

J = -1/m * sum(0.9 .* y .* log(hX) + 1.1 .* (identity - y) .* log(identity - hX));
grad = 1/m * (hX - y)' * X;

predicted = hX >= 0.5;
errors = y - predicted;
FP = length(find(errors < 0));
FN = length(find(errors > 0));
TP = length(find(y == 1 & predicted == 1));
TN = m - FP - FN - TP;

end