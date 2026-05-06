package org.endeavourhealth.recordviewer.common.dal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

class BaseJDBCDAL implements BaseDAL {
    private static final Logger LOG = LoggerFactory.getLogger(BaseJDBCDAL.class);

    @SuppressWarnings("resource")
    final Connection conn;
    protected BaseJDBCDAL (Connection connection){
        this.conn = connection;
    }
    public void beginTransaction() throws SQLException {
        conn.setAutoCommit(false);
    }

    public void commit() throws SQLException {
        conn.commit();
    }

    public void rollback() throws SQLException {
        conn.rollback();
    }

    @Override
    public void close() throws Exception {
        try {
            conn.setAutoCommit(true);
        } finally {
            ConnectionPool.getInstance().push(this.conn);
        }
    }
}
