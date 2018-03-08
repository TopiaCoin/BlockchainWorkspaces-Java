package io.topiacoin.model;

public class FileTag {

    private String scope;
    private String value;

    public FileTag() {
    }

    public FileTag(String scope, String value) {
        this.scope = scope;
        this.value = value;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileTag fileTag = (FileTag) o;

        if (scope != null ? !scope.equals(fileTag.scope) : fileTag.scope != null) return false;
        return value != null ? value.equals(fileTag.value) : fileTag.value == null;
    }

    @Override
    public int hashCode() {
        int result = scope != null ? scope.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FileTag{" +
                "scope='" + scope + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
