function plotData3D(X, y)
%PLOTDATA Plots the data points X and y into a new figure 
%   PLOTDATA(x,y) plots the data points with + for the positive examples
%   and o for the negative examples. X is assumed to be a Mx2 matrix.

% Create New Figure
figure; hold on;

% ====================== YOUR CODE HERE ======================
% Instructions: Plot the positive and negative examples on a
%               2D plot, using the option 'k+' for the positive
%               examples and 'ko' for the negative examples.
%

% positives = []
% negatives = []
% for i = 1:length(y)
% 	if (y(i))
% 		positives(end+1, :) = X(i, :)
% 	else 
% 		negatives(end+1, :) = X(i, :)
% 	end
% end

scatter3(X(:, 1), X(:, 2), X(:, 3), 5, y, 's')
% scatter3(negatives(:, 1), negatives(:, 2), negatives(:, 3), 'r')




% =========================================================================



hold off;

end
