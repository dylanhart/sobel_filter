img = getTitle();

selectWindow(img);
run("Duplicate...", "title=sobel_x");
run("32-bit");
run("Convolve...", "text1=[1 0 -1\n2 0 -2\n1 0 -1\n] normalize");
run("Duplicate...", "title=sobel_x2");
run("Square");

selectWindow(img);
run("Duplicate...", "title=sobel_y");
run("32-bit");
run("Convolve...", "text1=[1 2 1\n0 0 0\n-1 -2 -1\n] normalize");
run("Duplicate...", "title=sobel_y2");
run("Square");

imageCalculator("Add create 32-bit", "sobel_x2","sobel_y2");
selectWindow("Result of sobel_x2");
run("Square Root");
run("Enhance Contrast...", "saturated=0.3");
rename("sobel_mag");

close("sobel_x2");
close("sobel_y2");

selectImage("sobel_x"); 
w = getWidth(); 
h = getHeight(); 
x_img = newArray(w*h); 
y_img = newArray(w*h); 

for (y=0; y<h; y++) { 
for (x=0; x<w; x++) { 
x_img[x+(w*y)]=getPixel(x, y);
} 
} 

selectImage("sobel_y"); 
for (y=0; y<h; y++) { 
for (x=0; x<w; x++) { 
y_img[x+(w*y)]=getPixel(x, y); 
} 
} 

newImage("sobel_phase", "32-bit", w, h, 1); 
for (y=0; y<h; y++) { 
for (x=0; x<w; x++) { 
setPixel(x, y, 0.5*atan2(y_img[x+(w*y)],x_img[x+(w*y)])); 
} 
} 
updateDisplay();
run("Enhance Contrast...", "saturated=0.3");

close("sobel_x");
close("sobel_y");
