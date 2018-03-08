package io.topiacoin.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class MemberTest {


    @Test
    public void testDefaultConstructor() throws Exception {
        Member member = new Member() ;

        assertNull ( member.getUserID()) ;
        assertNull(member.getInviterID());
        assertEquals(0, member.getStatus());
        assertEquals(0, member.getInviteDate());
    }

    @Test
    public void testConstructor() throws Exception {
        String userID = "LeileeWingfeather";
        String inviterID = "JannerWingfeather";
        int status = 34;
        long date = 1234567698L;

        Member member = new Member(userID, status, date, inviterID) ;

        assertEquals(userID, member.getUserID());
        assertEquals(inviterID, member.getInviterID());
        assertEquals(status, member.getStatus());
        assertEquals(date, member.getInviteDate());
    }

    @Test
    public void testBasicAccessors() throws Exception {

        String userID = "LeileeWingfeather";
        String inviterID = "JannerWingfeather";
        int status = 34;
        long date = 1234567698L;

        Member member = new Member() ;

        assertNull ( member.getUserID()) ;
        member.setUserID(userID);
        assertEquals(userID, member.getUserID());
        member.setUserID(null);
        assertNull ( member.getUserID()) ;

        assertNull(member.getInviterID());
        member.setInviterID(inviterID);
        assertEquals(inviterID, member.getInviterID());
        member.setInviterID(null);
        assertNull(member.getInviterID());

        assertEquals(0, member.getStatus());
        member.setStatus(status);
        assertEquals(status, member.getStatus());
        member.setStatus(0);
        assertEquals(0, member.getStatus());

        assertEquals(0, member.getInviteDate());
        member.setInviteDate(date);
        assertEquals(date, member.getInviteDate());
        member.setInviteDate(0);
        assertEquals(0, member.getInviteDate());

    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        String userID = "LeileeWingfeather";
        String inviterID = "JannerWingfeather";
        int status = 34;
        long date = 1234567698L;

        Member member1 = new Member(userID, status, date, inviterID) ;
        Member member2 = new Member(userID, status, date, inviterID) ;

        assertEquals(member1, member1);
        assertEquals(member2, member2);
        assertEquals(member1, member2);
        assertEquals(member2, member1);

        assertEquals(member1.hashCode(), member2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {
        Member member1 = new Member() ;
        Member member2 = new Member() ;

        assertEquals(member1, member1);
        assertEquals(member2, member2);
        assertEquals(member1, member2);
        assertEquals(member2, member1);

        assertEquals(member1.hashCode(), member2.hashCode());
    }


}
