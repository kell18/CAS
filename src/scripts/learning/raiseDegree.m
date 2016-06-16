%% matchDegree: Raise degree of regression polynomial
function [dX] = raiseDegree(X, degree)
	ch = X(:, 2:end);
	dX = X;

	for d = 2:degree
		dX = [dX, ch .^ d];
	end
end
