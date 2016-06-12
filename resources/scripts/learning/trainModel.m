function [theta, fVal, info] = trainModel(costFn, th0)
theta = th0;
m = length(th0);

for i = 1:100
	thI = (rand(m, 1) .- 0.5) .* 10;
	[thI, fV, inf, out, grad, hess] = fminunc(costFn, thI);
	if (thI < theta)
		theta = thI;
		fVal = fV;
		info = inf;
	end
end

end
