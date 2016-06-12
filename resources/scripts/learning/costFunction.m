function [J, grad, FP, FN] = costFunction(theta, X, y)
%COSTFUNCTION Compute cost and gradient for logistic regression
%   J = COSTFUNCTION(theta, X, y) computes the cost of using theta as the
%   parameter for logistic regression and the gradient of the cost
%   w.r.t. to the parameters.

% Initialize some useful values
m = length(y); % number of training examples

% You need to return the following variables correctly 
J = 0;
grad = zeros(size(theta));

% ====================== YOUR CODE HERE ======================
% Instructions: Compute the cost of a particular choice of theta.
%               You should set J to the cost.
%               Compute the partial derivatives and set grad to the partial
%               derivatives of the cost w.r.t. each parameter in theta
%
% Note: grad should have the same dimensions as theta
%

identity = ones(m, 1);
hX = sigmoid(X * theta);
J = -1/m * sum(0.9 .* y .* log(hX) + 1.1 .* (identity - y) .* log(identity - hX));

grad = 1/m * (hX - y)' * X;

predicted = hX >= 0.5;
errors = y - predicted;
FP = length(find(errors < 0));
FN = length(find(errors > 0));

% =============================================================

end