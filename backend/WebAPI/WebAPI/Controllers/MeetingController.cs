using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Net.Http.Headers;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Security.Claims;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using WebAPI.Helpers;
using WebAPI.Models;

namespace WebAPI.Controllers
{
    [Authorize]
    [Route("api/[controller]")]
    [ApiController]
    public class MeetingController : Controller
    {
        SkuciSeDBContext ctx;

        private readonly string username;
        private readonly uint userId;

        public MeetingController(SkuciSeDBContext _ctx, IHttpContextAccessor httpContextAccessor)
        {
            ctx = _ctx;
            username = httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.Name)?.Value;
            uint.TryParse(httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier)?.Value, out userId);
        }

        [HttpPost]
        [Route("arrange_meeting")]
        public ActionResult<string> ArrangeMeeting(uint advertId, DateTime time)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            var result1 = ctx.Adverts.Where(ad => ad.Id == advertId && ad.OwnerId != userId).FirstOrDefault();

            if (result1 == null)
                return NotFound("Greška, nije moguće zakazati sastanak za ovaj oglas jer oglas ne postoji ili ste vi vlasnik.");

            if (DateTime.Now >= time)
                return BadRequest("Greška, predloženo vreme sastanka je prošlo.");

            var result = ctx.Meetings.Where(m => m.AdvertId == advertId && m.VisitorId == userId && m.Concluded == false);

            if (result.Any())
                return BadRequest("Greška, već ste zakazali sastanak za ovaj oglas.");

            Meeting newMeeting = new Meeting() { AdvertId = advertId, Time = time, VisitorId = userId, AgreedVisitor = true, DateCreated = DateTime.Now, AgreedOwner = false, Concluded = false };

            try
            {
                ctx.Meetings.Add(newMeeting);
                ctx.SaveChanges();
                return Ok("Predlog za sastanak uspešno poslat.");
            }
            catch
            {
                return StatusCode(500, "Greška pri zakazivanju sastanka.");
            }

        }

        [HttpPost]
        [Route("get_my_meetings")]
        public ActionResult<IEnumerable<object>> GetMyMeetings()
        {
            JwtHelper.TokenUnverified(userId, Request);

            //return ctx.Meetings.
            //    Join(ctx.Adverts, m => m.AdvertId, ad => ad.Id, (m, ad) => new { m, ad.OwnerId, ad.Title }).
            //    Where(m => (m.OwnerId == userId || m.m.VisitorId == userId) && m.m.Concluded == false).ToList();

            //var result = ctx.Meetings.
            //        Join(ctx.Adverts, m => m.AdvertId, ad => ad.Id, (m, ad) => new { m, ad.OwnerId, ad.Title }).
            //        Where(m => (m.OwnerId == userId || m.m.VisitorId == userId) && m.m.Concluded == false).
            //        Select(j => new { OwnerId = j.OwnerId, MeetingData = j.m, OtherUserId = j.OwnerId == userId ? j.m.VisitorId : j.OwnerId, AdvertTitle = j.Title}).
            //        Join(ctx.Users, j => j.OtherUserId, u => u.Id, (j, u) => new { MeetingData = j.MeetingData, OtherUserId = j.OtherUserId, OtherUsername = u.Username, AdvertTitle = j.AdvertTitle, AmIOwner = j.OwnerId == userId ? true : false }).ToList();

            var result = ctx.Meetings.
                    Join(ctx.Adverts, m => m.AdvertId, ad => ad.Id, (m, ad) => new { m, ad.OwnerId, ad.Title }).
                    Where(m => (m.OwnerId == userId || (m.m.VisitorId == userId && m.m.Time > DateTime.Now)) && m.m.Concluded == false).
                    Select(j => new { OwnerId = j.OwnerId, MeetingData = j.m, OtherUserId = j.OwnerId == userId ? j.m.VisitorId : j.OwnerId, AdvertTitle = j.Title }).
                    Join(ctx.Users, j => j.OtherUserId, u => u.Id, (j, u) => new { MeetingData = j.MeetingData, OtherUserId = j.OtherUserId, OtherUsername = u.Username, AdvertTitle = j.AdvertTitle, AmIOwner = j.OwnerId == userId ? true : false }).ToList();

            return result;
        }

        [HttpPost]
        [Route("confirm_meeting")]
        public ActionResult<string> ConfirmMeeting(uint meetingId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            Meeting meeting = ctx.Meetings.Where(m => m.Id == meetingId).FirstOrDefault();

            if (meeting == null)
                return NotFound("Greška, sastanak ne postoji.");


            if (meeting.VisitorId == userId)
                meeting.AgreedVisitor = true;
            else
                meeting.AgreedOwner = true;
            

            try
            {
                ctx.Meetings.Update(meeting);
                ctx.SaveChanges();

                return Ok("Sastanak uspešno prihvacen.");
            }
            catch
            {
                return StatusCode(500, "Greška pri prihvatanju sastanka.");
            }
        }

        [HttpPost]
        [Route("edit_meeting_proposal")]
        public ActionResult<string> EditMeeting(uint meetingId, DateTime newTime)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            Meeting result = ctx.Meetings.Where(m => m.Id == meetingId).FirstOrDefault();

            if (result == null)
                return NotFound("Greška, sastanak ne postoji.");

            if (DateTime.Now >= result.Time.AddDays(-1))
                return BadRequest("Greška, nije moguće izmeniti vreme sastanka 24h pre sastanka.");

            //if (result.AgreedOwner == true && result.AgreedVisitor == true)
            //    return BadRequest("Meeting time is already agreed upon.");

            if (result.Concluded == true)
                return BadRequest("Greška, sastanak je već završen.");

            result.Time = newTime;
            
            if(result.VisitorId == userId)
            {
                result.AgreedOwner = false;
                result.AgreedVisitor = true;
            }
            else
            {
                result.AgreedOwner = true;
                result.AgreedVisitor = false;
            }

            try
            {
                ctx.Meetings.Update(result);
                ctx.SaveChanges();
                return Ok("Predlog za novo vreme sastanka uspešno poslato.");
            }
            catch (Exception e)
            {
                return StatusCode(500, "Greška pri izmeni vremena sastanka.");
            }
        }

        [HttpPost]
        [Route("cancel_meeting")]
        public ActionResult<string> CancelMeeting(uint meetingId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            var result = ctx.Meetings.Where(m => m.Id == meetingId && m.Concluded == false).
                Join(ctx.Adverts, m => m.AdvertId, ad => ad.Id, (m, ad) => new { m, ad.OwnerId }).FirstOrDefault();

            if (result == null)
                return NotFound("Greška, sastanak ne postoji ili je već završen.");

            if (DateTime.Now >= result.m.Time.AddDays(-1))
                return BadRequest("Greška, nije moguće otkazati sastanak 24h pre sastanka.");

            if (result.m.VisitorId != userId && result.OwnerId != userId)
                return BadRequest("Greška, niste član ovog sastanka.");

            try
            {
                ctx.Meetings.Remove(result.m);
                ctx.SaveChanges();
                return Ok("Sastanak uspešno otkazan.");
            }
            catch (Exception e)
            {
                return StatusCode(500, "Greška pri otkazivanju sastanka.");
            }
        }

        //[HttpPost]
        //[Route("get_ended_meetings")]
        //public ActionResult<IEnumerable<Meeting>> GetMyEndedMeetings()
        //{
        //    if (JwtHelper.TokenUnverified(userId, Request))
        //        return Unauthorized();

        //    return ctx.Meetings.
        //        Join(ctx.Adverts, m => m.AdvertId, ad => ad.Id, (m, ad) => new { m, ad.OwnerId }).
        //        Where(j => j.OwnerId == userId && j.m.Concluded == false && j.m.Time <= DateTime.Now).Select(j => j.m).ToList();
        //}

        [HttpPost]
        [Route("conclude_meeting")]
        public ActionResult<string> ConcludeMeeting(uint meetingId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            Meeting result = ctx.Meetings.Where(m => m.Id == meetingId && ctx.Adverts.Any(ad => ad.OwnerId == userId)).FirstOrDefault();

            if (result == null)
                return NotFound("Greška, sastanak ne postoji.");

            if (result.Time > DateTime.Now)
                return BadRequest("Greška, sastanak još uvek nije počeo.");

            if (result.Concluded == true)
                return BadRequest("Greška, sastanak je već završen.");

            result.Concluded = true;

            try
            {
                ctx.Meetings.Update(result);
                ctx.SaveChanges();
                return Ok("Sastanak uspešno završen.");
            }
            catch (Exception e)
            {
                return StatusCode(500, "Greška pri završavanju sastanka.");
            }
        }

        [HttpPost]
        [Route("delete_meeting")]
        public ActionResult<string> DeleteMeeting(uint meetingId)
        {
            if (JwtHelper.TokenUnverified(userId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            Meeting result = ctx.Meetings.Where(m => m.Id == meetingId && ctx.Adverts.Any(ad => ad.OwnerId == userId) && m.Concluded == false).FirstOrDefault();

            if (result == null)
                return NotFound("Greška, sastanak ne postoji ili je već završen.");

            if (result.Time > DateTime.Now)
                return BadRequest("Greška, sastanak još uvek nije počeo.");

            try
            {
                ctx.Meetings.Remove(result);
                ctx.SaveChanges();
                return Ok("Sastanak uspešno obrisan.");
            }
            catch (Exception e)
            {
                return StatusCode(500, "Greška pri brisanju sastanka.");
            }
        }
    }
}
