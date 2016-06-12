function[X, y] = prepareClassData(path)

D = load(path);
X = D(:, 1:end-1);
y = D(:, end);


end