%% lowerDegree: Decreese degree of regression polynomial
function [dX] = lowerDegree(X, degree)
	ch = X(:, 2:end);
	dX = X;
  
	for d = 2:degree
    dX = [dX, dX(:, d-1) .* X];
    % dX = [dX, -log10(ch .^ d)];
		% dX = [dX, (ch) .^ (d)];
    % dX = [dX, (ch) .^ (1/d)];
    % dX = [dX, (ch .+ 1) .^ (d)];
	end
end
