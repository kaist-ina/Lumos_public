/* This file was generated by SableCC (http://www.sablecc.org/). */

package soot.jimple.parser.node;

import soot.jimple.parser.analysis.*;

@SuppressWarnings("nls")
public final class TCls extends Token
{
    public TCls()
    {
        super.setText("cls");
    }

    public TCls(int line, int pos)
    {
        super.setText("cls");
        setLine(line);
        setPos(pos);
    }

    @Override
    public Object clone()
    {
      return new TCls(getLine(), getPos());
    }

    @Override
    public void apply(Switch sw)
    {
        ((Analysis) sw).caseTCls(this);
    }

    @Override
    public void setText(@SuppressWarnings("unused") String text)
    {
        throw new RuntimeException("Cannot change TCls text.");
    }
}
