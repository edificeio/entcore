package org.entcore.common.s3.dataclasses;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Part")
public class CompletePart {

    @XmlElement(name = "ETag")
    protected String eTag;

    @XmlElement(name = "PartNumber")
    protected int partNumber;

    public CompletePart() {}

    public CompletePart(String eTag, int partNumber) {
        this.eTag = eTag;
        this.partNumber = partNumber;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public int getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }

}
