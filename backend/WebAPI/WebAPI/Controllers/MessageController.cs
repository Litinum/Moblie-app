using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Net.Http.Headers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using WebAPI.Helpers;
using WebAPI.Models;

namespace WebAPI.Controllers
{
    [Authorize]
    [Route("api/[controller]")]
    public class MessageController : Controller
    {
        SkuciSeDBContext ctx;
        private readonly uint userId;

        public MessageController(SkuciSeDBContext _ctx, IHttpContextAccessor httpContextAccessor)
        {
            ctx = _ctx;
            userId = uint.Parse(httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier).Value);
        }

        [HttpPost]
        [Route("get_chats")]
        public ActionResult<IEnumerable<object>> GetChats() // Returns a list of users who have sent or received messages from the currrent user, and the latest message in the chat
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized();

            // Messages
            var otherUsersWithMessages = ctx.Messages
                .Where(msg => msg.SenderId == userId || msg.ReceiverId == userId)
                .Select(msg => new { OtherUserId = msg.SenderId != userId ? msg.SenderId : msg.ReceiverId, Message = msg })
                .Distinct();

            var otherUsersWithLatestMessageDate = otherUsersWithMessages
                .GroupBy(o => o.OtherUserId)
                .Select(g => new { OtherUserId = g.Key, MaxDate = g.Max(j => j.Message.SendDate) });

            var otherUsersWithLatestMessage = otherUsersWithMessages
                .Where(m => otherUsersWithLatestMessageDate.Any(o => o.OtherUserId == m.OtherUserId && m.Message.SendDate == o.MaxDate))
                .Select(w => new { w.OtherUserId, w.Message })
                .OrderByDescending(x => x.Message.SendDate);

            // Meetings
            var otherUsersWithMeetings = ctx.Meetings
                .Join(
                    ctx.Adverts,
                    meet => meet.AdvertId,
                    ad => ad.Id,
                    (meet, ad) => new { meet, ad }
                    )
                .Where(j => j.meet.VisitorId == userId || j.ad.OwnerId == userId)
                .Select(j => new { OtherUserId = j.meet.VisitorId != userId ? j.meet.VisitorId : j.ad.OwnerId, Meeting = j.meet })
                .Distinct();

            var otherUsersWithLatestMeetingDate = otherUsersWithMeetings
                .GroupBy(o => o.OtherUserId)
                .Select(g => new { OtherUserId = g.Key, MaxDate = g.Max(j => j.Meeting.DateCreated) });

            var otherUsersWithLatestMeeting = otherUsersWithMeetings
                .Where(m => otherUsersWithLatestMeetingDate.Any(o => o.OtherUserId == m.OtherUserId && m.Meeting.DateCreated == o.MaxDate))
                .Select(w => new { w.OtherUserId, w.Meeting })
                .Join(ctx.Adverts,
                        w => w.Meeting.AdvertId,
                        a => a.Id,
                        (w, a) => new { OtherUserId = w.OtherUserId, Meeting = w.Meeting, Advert = a })
                .Join(ctx.Users,
                        j => j.OtherUserId,
                        u => u.Id,
                        (j, u) => new MeetingDisplay
                        {
                            id = j.Meeting.Id,
                            advertId = j.Meeting.AdvertId,
                            otherUser = j.OtherUserId,
                            username = u.Username,
                            title = j.Advert.Title,
                            proposedTime = j.Meeting.Time,
                            dateCreated = j.Meeting.DateCreated,
                            agreedVisitor = j.Meeting.AgreedVisitor,
                            agreedOwner = j.Meeting.AgreedOwner,
                            concluded = j.Meeting.Concluded,
                            owner = j.Advert.OwnerId == userId ? true : false
                        }).Select(j => new { OtherUserId = j.otherUser, MeetingDisplay = j })
                        .OrderByDescending(x => x.MeetingDisplay.dateCreated);

            List<object> rMsgs = new List<object>();
            foreach (var rm in otherUsersWithLatestMeeting)
            {
                MessageOrMeeting m = new MessageOrMeeting();
                var message = otherUsersWithLatestMessage.Where(o => o.OtherUserId == rm.OtherUserId).FirstOrDefault();
                if (message != null && message.Message.SendDate > rm.MeetingDisplay.dateCreated)
                {
                    m.Meeting = null;
                    m.Message = message.Message;
                    m.IsMessage = true;
                }
                else
                {
                    m.Meeting = rm.MeetingDisplay;
                    m.Message = null;
                    m.IsMessage = false;
                }
                rMsgs.Add(new { User = GetUserDisplay(rm.OtherUserId), Message = m });
            }

            return rMsgs;
        }

        [HttpPost]
        [Route("get_chat")]
        public ActionResult<IEnumerable<MessageOrMeeting>> GetChat(uint otherUserId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            var messages = ctx.Messages
                .Where(m => (m.SenderId == otherUserId && m.ReceiverId == userId) || (m.SenderId == userId && m.ReceiverId == otherUserId))
                .OrderBy(m => m.SendDate).ToList();

            var meetings = ctx.Meetings.
                    Join(ctx.Adverts, m => m.AdvertId, ad => ad.Id, (m, ad) => new { m, ad.OwnerId, ad.Title }).
                    Where(m => (m.OwnerId == userId && m.m.VisitorId == otherUserId) || (m.m.VisitorId == userId && m.OwnerId == otherUserId)).
                    Select(j => new { OwnerId = j.OwnerId, MeetingData = j.m, OtherUserId = j.OwnerId == userId ? j.m.VisitorId : j.OwnerId, AdvertTitle = j.Title }).
                    Join(ctx.Users, 
                        j => j.OtherUserId, 
                        u => u.Id, 
                        (j, u) => new MeetingDisplay {
                            id = j.MeetingData.Id,
                            advertId = j.MeetingData.AdvertId,
                            otherUser = j.OtherUserId,
                            username = u.Username,
                            title = j.AdvertTitle,
                            proposedTime = j.MeetingData.Time,
                            dateCreated = j.MeetingData.DateCreated,
                            agreedVisitor = j.MeetingData.AgreedVisitor,
                            agreedOwner = j.MeetingData.AgreedOwner,
                            concluded = j.MeetingData.Concluded,
                            owner = j.OwnerId == userId ? true : false
                        })
                    .OrderBy(m => m.dateCreated).ToList();

            for (int i = 0; i < messages.Count; i++)
            {
                if (messages[i].ReceiverId == userId)
                {
                    messages[i].Seen = true;
                    ctx.Messages.Update(messages[i]);
                }
            }

            ctx.SaveChanges();

            List<MessageOrMeeting> result = new List<MessageOrMeeting>();
            int total = messages.Count + meetings.Count;
            for (int i = 0; i < total; i++)
            {
                if (messages.Count > 0 && meetings.Count > 0)
                {
                    if (messages[0].SendDate < meetings[0].dateCreated)
                    {
                        result.Add(new MessageOrMeeting
                        {
                            Message = messages[0],
                            Meeting = null,
                            IsMessage = true
                        });
                        messages = messages.Skip(1).ToList();
                    }
                    else
                    {
                        result.Add(new MessageOrMeeting
                        {
                            Message = null,
                            Meeting = meetings[0],
                            IsMessage = false
                        });
                        meetings = meetings.Skip(1).ToList();
                    }
                }
                else
                {
                    if(messages.Count > 0)
                    {
                        result.Add(new MessageOrMeeting
                        {
                            Message = messages[0],
                            Meeting = null,
                            IsMessage = true
                        });
                        messages = messages.Skip(1).ToList();
                    }
                    else
                    {
                        result.Add(new MessageOrMeeting
                        {
                            Message = null,
                            Meeting = meetings[0],
                            IsMessage = false
                        });
                        meetings = meetings.Skip(1).ToList();
                    }
                }
            }

            return result;
        }

        [HttpPost]
        [Route("send_message")]
        public IActionResult SendMessage(uint otherUserId, string content)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            if (otherUserId == userId)
                return BadRequest("Greška, ne možete poslati poruku samom sebi.");

            var hasMeeting = ctx.Meetings
                .Where(m => 
                    ((m.VisitorId == otherUserId && ctx.Adverts.Where(a => a.Id == m.AdvertId).FirstOrDefault().OwnerId == userId)
                    || ((m.VisitorId == userId && ctx.Adverts.Where(a => a.Id == m.AdvertId).FirstOrDefault().OwnerId == otherUserId))));

            if (!hasMeeting.Any())
                return BadRequest("Greška, nemate zakazan sastanak sa ovom osobom.");

            Message msg = new Message { SenderId = userId, ReceiverId = otherUserId, Content = content, Seen = false, SendDate = DateTime.Now };

            try
            {
                ctx.Messages.Add(msg);
                ctx.SaveChanges();

                return Ok();
            }
            catch
            {
                return StatusCode(500);
            }
        }

        [HttpPost]
        [Route("check_messages")]
        public ActionResult<int> CheckMessages()
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return ctx.Messages.Where(m => m.ReceiverId == userId && m.Seen == false).Count();
        }

        [HttpPost]
        [Route("get_user_status")]
        public ActionResult<UserDisplay> GetUserInfo(uint otherUserId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return GetUserDisplay(otherUserId);
        }

        public UserDisplay GetUserDisplay(uint id)
        {
            UserDisplay display = new UserDisplay();
            display.Id = id;
            display.Online = JwtHelper.CheckActiveToken(id) != null;
            User user = ctx.Users.Find(id);
            display.DisplayName = (user.FirstName + " " + user.LastName).Trim();

            return display;
        }
    }
}
