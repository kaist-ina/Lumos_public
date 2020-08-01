package ExecutionGenerator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.*;

import java.io.File;

public class ManifestParser {
    public static void main (String argv[]) {

        try {

            File file = new File("D:\\Dropbox\\Dropbox\\IoT_Research\\jadx-0.6.1\\bin\\hue\\AndroidManifest.xml");

            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());

            if (doc.hasChildNodes()) {
                printNote(doc.getChildNodes());
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    private static void printNote(NodeList nodeList) {

        for (int count = 0; count < nodeList.getLength(); count++) {

            Node tempNode = nodeList.item(count);

            // make sure it's element node.
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {

                // get node name and value
                if (tempNode.getNodeName().equals("activity")) {
//                    System.out.println("\nNode Name =" + tempNode.getNodeName() + " [OPEN]");
//                    System.out.println("Node Value =" + tempNode.getTextContent());
                }

                if (tempNode.getNodeName().equals("activity") && tempNode.hasAttributes()) {

                    // get attributes names and values
                    NamedNodeMap nodeMap = tempNode.getAttributes();

                    for (int i = 0; i < nodeMap.getLength(); i++) {
                            Node node = nodeMap.item(i);
                        if (node.getNodeName().toString().equals("android:name")) {
//                            System.out.println("attr name : " + node.getNodeName());
                            System.out.println("attr value : " + node.getNodeValue());
                        }
                    }

                }
                if (tempNode.hasChildNodes()) {
                    // loop again if has child nodes
                    printNote(tempNode.getChildNodes());

                }
                if (tempNode.getNodeName().equals("activity")) {
//                    System.out.println("Node Name =" + tempNode.getNodeName() + " [CLOSE]");
                }
            }
        }
    }
}
