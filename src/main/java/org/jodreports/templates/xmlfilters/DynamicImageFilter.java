package org.jodreports.templates.xmlfilters;

import org.jodreports.opendocument.OpenDocumentNamespaces;
import org.jodreports.templates.TemplateFreemarkerNamespace;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes content.xml for dynamic images<p>
 * Only images enclosed in a draw:frame with a name starting with {@link #IMAGE_NAME_PREFIX} and ending with {@link #IMAGE_NAME_SUFFIX} will be processed
 */
public class DynamicImageFilter extends XmlEntryFilter {

	/**
	 * Only images enclosed in a draw:frame with a name starting with
	 * this prefix will be processed 
	 */
	public static final String IMAGE_NAME_PREFIX = TemplateFreemarkerNamespace.NAME + ".image(";

	/**
	 * Only images enclosed in a draw:frame with a name ending with
	 * this suffix will be processed 
	 */
	public static final String IMAGE_NAME_SUFFIX = ")";

	private static final String IMAGE_WIDTH_PREFIX	= TemplateFreemarkerNamespace.NAME + ".imageWidth(";
	private static final String IMAGE_HEIGHT_PREFIX = TemplateFreemarkerNamespace.NAME + ".imageHeight(";

	private static final Logger log = LoggerFactory.getLogger(DynamicImageFilter.class);

	public void doFilter(Document document) {
		Nodes nodes = document.query("//draw:image", OpenDocumentNamespaces.XPATH_CONTEXT);
		for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
			Element imageElement = (Element) nodes.get(nodeIndex);
			Element frameElement = (Element) imageElement.getParent();
			if (!"draw:frame".equals(frameElement.getQualifiedName())) {
				log.warn("draw:image not inside a draw:frame? skipping");
				continue;
			}
			String frameName = frameElement.getAttributeValue("name", OpenDocumentNamespaces.URI_DRAW);
			if (frameName != null && 
					frameName.toLowerCase().startsWith(IMAGE_NAME_PREFIX) && 
					frameName.endsWith(IMAGE_NAME_SUFFIX)) {
				Attribute hrefAttribute = imageElement.getAttribute("href", OpenDocumentNamespaces.URI_XLINK);
				String defaultImageName = (hrefAttribute != null) ? hrefAttribute.getValue() : "";
				if (hrefAttribute == null) {
					hrefAttribute = new Attribute("xlink:href", OpenDocumentNamespaces.URI_XLINK, "");
					imageElement.addAttribute(hrefAttribute);
					imageElement.addAttribute(new Attribute("xlink:type", OpenDocumentNamespaces.URI_XLINK, "simple"));
					imageElement.addAttribute(new Attribute("xlink:show", OpenDocumentNamespaces.URI_XLINK, "embed"));
					imageElement.addAttribute(new Attribute("xlink:actuate", OpenDocumentNamespaces.URI_XLINK, "onLoad"));
				}
				if (frameName.contains(",")) {
					Attribute widthAttribute = frameElement.getAttribute("width", OpenDocumentNamespaces.URI_SVG);
					Attribute heightAttribute = frameElement.getAttribute("height", OpenDocumentNamespaces.URI_SVG);
					String maxSize = ",'" + widthAttribute.getValue().trim() + "','" + 
											heightAttribute.getValue().trim() + "',";
					String sizeParameters = frameName.replaceFirst(",", maxSize);
					widthAttribute.setValue("${" + sizeParameters.replace(IMAGE_NAME_PREFIX, 
							IMAGE_WIDTH_PREFIX + "'" + defaultImageName + "',") + "}");
					heightAttribute.setValue("${" + sizeParameters.replace(IMAGE_NAME_PREFIX, 
							IMAGE_HEIGHT_PREFIX + "'" + defaultImageName + "',") + "}");
					frameName = frameName.split(",")[0] + IMAGE_NAME_SUFFIX;
				}
				String imageNameVariable = "${" 
						+ frameName.replace(IMAGE_NAME_PREFIX, IMAGE_NAME_PREFIX + "'" + defaultImageName + "',") 
						+ "}";
				hrefAttribute.setValue(imageNameVariable);
				frameElement.getAttribute("name", OpenDocumentNamespaces.URI_DRAW).setValue(imageNameVariable);
			}
		}
	}
}
