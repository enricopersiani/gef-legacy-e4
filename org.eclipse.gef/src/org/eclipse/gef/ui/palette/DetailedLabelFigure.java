package org.eclipse.gef.ui.palette;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.TextFlow;

/**
 * A customized figure used to represent entries in the GEF Palette.
 * 
 * @author Pratik Shah
 */
public class DetailedLabelFigure 
	extends Figure 
{

/**
 * Constant that indicates that this figure is not selected
 */
public static final int NOT_SELECTED = 0;
/**
 * Constant that indicates that this figure is selected and has focus
 */
public static final int SELECTED_WITH_FOCUS = 1;
/**
 * Constant that indicates that this figure is selected but does not have focus
 */
public static final int SELECTED_WITHOUT_FOCUS = 2;

public static final String SELECTED_PROPERTY = "selected"; //$NON-NLS-1$

private Image shadedIcon;
private ImageFigure image;
private FlowPage page;
private TextFlow nameText, descText;
private Font boldFont;
private boolean useLargeIcons;
private int selectionState, layoutMode = -1;
private List listeners = new ArrayList();

/**
 * Constructor
 */
public DetailedLabelFigure() {
	image = new SelectableImageFigure();
	image.setAlignment(PositionConstants.NORTH);
	page = new FocusableFlowPage();
	page.setBorder(new MarginBorder(0, 2, 0, 2));
	nameText = new TextFlow();
	descText = new TextFlow();
	page.add(nameText);
	add(image);
	add(page);
	BorderLayout layout = new BorderLayout();
	layout.setHorizontalSpacing(2);
	layout.setVerticalSpacing(2);
	setLayoutManager(layout);
}

public void addChangeListener(ChangeListener listener) {
	listeners.add(listener);
}

protected void fireStateChanged(String property) {
	Iterator iter = listeners.iterator();
	ChangeEvent change = new ChangeEvent(this, property);
	while (iter.hasNext())
		((ChangeListener)iter.next()).
			handleStateChanged(change);
}

/**
 * @see java.lang.Object#finalize()
 */
protected void finalize() throws Throwable {
	if (boldFont != null) {
		nameText.setFont(null);
		boldFont.dispose();
		boldFont = null;
	}
}

/** * @return whether this figure is selected or not */
public boolean isSelected() {
	return selectionState == SELECTED_WITH_FOCUS 
	     || selectionState == SELECTED_WITHOUT_FOCUS;
}

public void removeChangeListener(ChangeListener listener) {
	listeners.remove(listener);
}

public void setDescription(String s) {
	/*
	 * @TODO:Pratik
	 * Does this string need to be externalized?
	 */
	descText.setText(" - " + s); //$NON-NLS-1$
}

/**
 * Sets the icon for this figure
 * 
 * @param icon The new image */
public void setImage(Image icon) {
	image.setImage(icon);
}

public void setLayoutMode(int layoutMode) {
	updateFont(layoutMode);
	
	if (layoutMode == this.layoutMode) {
		return;
	}
	
	this.layoutMode = layoutMode;

	add(page);
	if (page.getChildren().contains(descText)) {
		page.remove(descText);
	}
	
	BorderLayout layout = (BorderLayout) getLayoutManager();
	if (layoutMode == PaletteViewerPreferences.LAYOUT_FOLDER) {
		page.setHorizontalAligment(PositionConstants.CENTER);
		layout.setConstraint(image, BorderLayout.TOP);
		layout.setConstraint(page, BorderLayout.CENTER);
	} else if (layoutMode == PaletteViewerPreferences.LAYOUT_ICONS) {
		layout.setConstraint(image, BorderLayout.CENTER);
		remove(page);
	} else if (layoutMode == PaletteViewerPreferences.LAYOUT_LIST) {
		page.setHorizontalAligment(PositionConstants.LEFT);
		layout.setConstraint(image, BorderLayout.LEFT);
		layout.setConstraint(page, BorderLayout.CENTER);
	} else if (layoutMode == PaletteViewerPreferences.LAYOUT_DETAILS) {
		page.add(descText);
		page.setHorizontalAligment(PositionConstants.LEFT);
		layout.setConstraint(image, BorderLayout.LEFT);
		layout.setConstraint(page, BorderLayout.CENTER);
	}
}

public void setName(String str) {
	nameText.setText(str);
}

public void setSelected(int state) {
	selectionState = state;
	if (selectionState == SELECTED_WITH_FOCUS) {
		setForegroundColor(ColorConstants.menuForegroundSelected);
		setBackgroundColor(ColorConstants.menuBackgroundSelected);
		fireStateChanged(SELECTED_PROPERTY);
	} else if (selectionState == SELECTED_WITHOUT_FOCUS) {
		setForegroundColor(null);
		setBackgroundColor(ColorConstants.button);
		fireStateChanged(SELECTED_PROPERTY);
	} else {
		setForegroundColor(null);
		setBackgroundColor(null);
	}
}

/**
 * Creates an ImageData representing the given <code>Image</code> shaded with the given
 * <code>Color</code>.
 * 
 * @param fromImage	Image that has to be shaded
 * @param shade		The Color to be used for shading
 * @return A new ImageData that can be used to create an Image.
 */	
public static ImageData createShadedImage(Image fromImage, Color shade) {
	org.eclipse.swt.graphics.Rectangle r = fromImage.getBounds();
	ImageData data = fromImage.getImageData();
	PaletteData palette = data.palette;
	if (!palette.isDirect) {
		/* Convert the palette entries */
		RGB [] rgbs = palette.getRGBs();
		for (int i = 0; i < rgbs.length; i++) {
			if (data.transparentPixel != i) {
				RGB color = rgbs [i];
				color.red = determineShading(color.red, shade.getRed());				
				color.blue = determineShading(color.blue, shade.getBlue());
				color.green = determineShading(color.green, shade.getGreen());
			}
		}
		data.palette = new PaletteData(rgbs);
	} else {
		/* Convert the pixels. */
		int[] scanline = new int[r.width];
		int redMask = palette.redMask;
		int greenMask = palette.greenMask;
		int blueMask = palette.blueMask;
		int redShift = palette.redShift;
		int greenShift = palette.greenShift;
		int blueShift = palette.blueShift;
		for (int y = 0; y < r.height; y++) {
			data.getPixels(0, y, r.width, scanline, 0);
			for (int x = 0; x < r.width; x++) {
				int pixel = scanline[x];
				int red = pixel & redMask;
				red = (redShift < 0) ? red >>> -redShift : red << redShift;
				int green = pixel & greenMask;
				green = (greenShift < 0) ? green >>> -greenShift : green << greenShift;
				int blue = pixel & blueMask;
				blue = (blueShift < 0) ? blue >>> -blueShift : blue << blueShift;
				red = determineShading(red, shade.getRed());
				blue = determineShading(blue, shade.getBlue());
				green = determineShading(green, shade.getGreen());
				red = (redShift < 0) ? red << -redShift : red >> redShift;
				green = (greenShift < 0) ? green << -greenShift : green >> greenShift;
				blue = (blueShift < 0) ? blue << -blueShift : blue >> blueShift;				
				scanline[x] = red | blue | green;
			}
			data.setPixels(0, y, r.width, scanline, 0);
		}
	}
	return data;
}

private void updateFont(int layout){
	if (boldFont != null) {
		nameText.setFont(null);
		boldFont.dispose();
		boldFont = null;
	}
	if (layout == PaletteViewerPreferences.LAYOUT_DETAILS) {
		/*
		 * @TODO:Pratik
		 * Find out if getting the first FontData in the array is valid.
		 * You also need to figure out a way of disposing this Font.  Right now you are
		 * doing it through the finalize method.
		 */
		FontData data = getFont().getFontData()[0];
		data.setStyle(SWT.BOLD);
		boldFont = new Font(Display.getCurrent(), data);
		nameText.setFont(boldFont);
	}
}

private static int determineShading(int origColor, int shadeColor) {
	return (origColor + shadeColor) / 2;
}

private class FocusableFlowPage extends FlowPage {
	protected void paintFigure(Graphics g) {
		if (selectionState == SELECTED_WITH_FOCUS) {
			Rectangle childBounds = null;
			List children = getChildren();
			for (int i = 0; i < children.size(); i++) {
				Figure child = (Figure) children.get(i);
				if (i == 0) {
					childBounds = new Rectangle(child.getBounds());
				} else {
					childBounds.union(child.getBounds());
				}
			}
			childBounds.crop(new Insets(0, -2, 1, -1)); // Right is -1 and not -2 to not leave
					                                    // too much space on the right
			translateToParent(childBounds);
			g.fillRectangle(childBounds);
			super.paintFigure(g);
			g.setForegroundColor(ColorConstants.black);
			g.setBackgroundColor(ColorConstants.white);
			g.drawFocus(childBounds.resize(-1, -1));
		}
	}
}

private class SelectableImageFigure extends ImageFigure {
	private Image shadedImage;
	private void disposeShadedImage() {
		if (shadedImage != null) {
			shadedImage.dispose();
			shadedImage = null;
		}
	}
	protected void finalize() throws Throwable {
		disposeShadedImage();
	}
	public Image getImage() {
		if (isSelected()) {
			if (shadedImage == null) {
				ImageData data = createShadedImage(super.getImage(), 
						ColorConstants.menuBackgroundSelected);
				shadedImage = new Image(null, data);
			}
			return shadedImage;
		}
		return super.getImage();
	}
	public void setImage(Image image) {
		if (image != super.getImage()) {
			disposeShadedImage();
		}
		super.setImage(image);
	}
}

}
