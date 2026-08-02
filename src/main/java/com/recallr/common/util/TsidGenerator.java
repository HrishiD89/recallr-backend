package com.recallr.common.util;

import com.github.f4b6a3.tsid.TsidCreator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class TsidGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return TsidCreator.getTsid().toLong();
    }
}