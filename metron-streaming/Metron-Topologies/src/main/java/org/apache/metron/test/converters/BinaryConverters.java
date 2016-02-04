package org.apache.metron.test.converters;

/**
 * Created by cstella on 1/27/16.
 */
public enum BinaryConverters implements IConverter {
    DEFAULT(new IConverter() {

        public byte[] convert(String s) {
            return s.getBytes();
        }
    })
    , FROM_HEX_STRING(new HexStringConverter());
    IConverter _underlying;
    BinaryConverters(IConverter i) {
        _underlying = i;
    }

    public byte[] convert(String s) {
        return _underlying.convert(s);
    }

}
