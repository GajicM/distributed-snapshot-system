package app.snapshot_bitcake;

import app.AppConfig;

import java.io.Serializable;
import java.util.Objects;

public class SnapshotVersionId implements Serializable {

        private static final long serialVersionUID = 112323L;

        private  int versionId;
        private int initiatorId;

        public SnapshotVersionId(int versionId) {
            this.versionId = versionId;
            if(versionId==0) //ako je pocetna verzija, inicijator ne postoji
                this.initiatorId = -1;
        }
        public SnapshotVersionId(int versionId, int initiatorId) {
            this.versionId = versionId;
            this.initiatorId = initiatorId;
        }

        public int getVersionId() {
            return versionId;
        }

    public void setInitiatorId(int initiatorId) {
        this.initiatorId = initiatorId;
    }

    public void setVersionId(int versionId) {
        this.versionId = versionId;
    }

    @Override
    public String toString() {
        return "ver:" + versionId + "_init:" + initiatorId;
    }

    @Override
    public boolean equals(Object obj) {
            if(obj == null) return false;
            if(!(obj instanceof SnapshotVersionId)) return false;
        return versionId == ((SnapshotVersionId)obj).getVersionId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionId);
    }

    public int getInitiatorId() {
        return initiatorId;
    }
}
