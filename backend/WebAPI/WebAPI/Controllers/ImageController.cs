using Microsoft.AspNetCore.Mvc;
using System;
using System.Collections.Generic;
using System.Linq;
using WebAPI.Services;
using WebAPI.Models;
using Microsoft.AspNetCore.Http;
using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using System.Text.Json;
using System.IO;
using Microsoft.Extensions.Hosting.Internal;
using WebAPI.Helpers;

namespace WebAPI.Controllers
{
    [Authorize]
    [Route("api/[controller]")]
    [ApiController]
    public class ImageController : Controller
    {
        private readonly SkuciSeDBContext ctx;
        private readonly SkuciSeImageService img;
        private readonly uint currentUserId;

        public ImageController(SkuciSeDBContext _ctx, SkuciSeImageService _img, IHttpContextAccessor httpContextAccessor)
        {
            ctx = _ctx;
            img = _img;
            //username = httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.Name)?.Value;
            uint.TryParse(httpContextAccessor.HttpContext.User.FindFirst(ClaimTypes.NameIdentifier)?.Value, out currentUserId);
        }

        [HttpPost]
        [Route("get_user_image")]
        public ActionResult<FileData> GetUserImage(uint userId = 0)
        {
            if (JwtHelper.TokenUnverified(currentUserId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            if (userId == 0) 
                userId = currentUserId;
            try
            {
                return img.GetUserImage(userId);
            }catch(Exception e)
            {
                return StatusCode(500, e);
            }
        }


        [HttpGet]
        [AllowAnonymous]
        [Route("get_user_image_file")]
        public IActionResult GetUserImageFile(uint userId = 0)
        {
            try
            {
                string dir = img.GetUserPath(userId);
                string file = img.GetDefaultUserImagePath();
                if (Directory.Exists(dir))
                {
                    string foundFile = Directory.GetFiles(dir).FirstOrDefault();
                    if (foundFile != null)
                        file = foundFile;
                }
                FileInfo fi = new FileInfo(file);

                string ext = fi.Extension.Trim(trimChar: '.');
                if (ext == "jpg") ext = "jpeg";

                return PhysicalFile(fi.FullName, $"image/{ext}");
            }
            catch (Exception e)
            {
                return StatusCode(500, e);
            }
        }

        [HttpGet]
        [AllowAnonymous]
        [Route("get_advert_image_file")]
        public IActionResult GetAdvertImageFile(uint advertId, string imageName)
        {
            try
            {
                string dir = img.GetAdvertPath(advertId);
                
                FileInfo fi = new FileInfo(Path.Join(dir, imageName));

                string ext = fi.Extension.Trim(trimChar: '.');
                if (ext == "jpg") ext = "jpeg";

                return PhysicalFile(fi.FullName, $"image/{ext}");
            }
            catch (Exception e)
            {
                return StatusCode(500, e);
            }
        }

        [HttpGet]
        [AllowAnonymous]
        [Route("get_advert_image_names")]
        public ActionResult<IEnumerable<string>> GetAdvertImageNames(uint advertId)
        {
            try
            {
                string dir = img.GetAdvertPath(advertId);
                if (Directory.Exists(dir))
                {
                    return Directory.GetFiles(dir).Select(f => (new FileInfo(f)).Name).ToArray();
                }
                else
                    return Array.Empty<string>();
            }
            catch (Exception e)
            {
                return StatusCode(500, e);
            }
        }

        [HttpPut]
        [Route("set_user_image")]
        public ActionResult SetUserImage([FromBody] IEnumerable<FileData> image)
        {
            if (JwtHelper.TokenUnverified(currentUserId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            if (image == null) { return BadRequest("Object must not be null!"); }

            try
            {
                //FileData imageData = JsonSerializer.Deserialize<FileData>(image);
                try
                {
                    img.DeleteUserImage(currentUserId);
                    FileData file = image.FirstOrDefault();
                    img.SetUserImage(currentUserId, file);
                }
                catch(Exception e)
                {
                    return StatusCode(500, e);
                }
                return Ok();
            }
            catch (Exception e)
            {
                return BadRequest(e);
            }
        }

        [HttpDelete]
        [Route("delete_user_image")]
        public ActionResult DeleteUserImage()
        {
            if (JwtHelper.TokenUnverified(currentUserId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            try
            {
                img.DeleteUserImage(currentUserId);
                return Ok();
            }
            catch (Exception e)
            {
                return StatusCode(500, e);
            }
        }

        [HttpPost]
        [Route("get_advert_images")]
        public ActionResult<List<FileData>> GetAdvertImages(uint advertId)
        {
            if (JwtHelper.TokenUnverified(currentUserId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            try
            {
                return img.GetAdvertImages(advertId);
            }
            catch (Exception e)
            {
                return StatusCode(500, e);
            }
        }

        private ActionResult TryEditAdvert(uint advertId, Action edit)
        {
            if (ctx.Adverts.Where(ad => ad.Id == advertId).FirstOrDefault().OwnerId == currentUserId)
            {
                try
                {
                    edit.Invoke();
                    return Ok();
                }
                catch (Exception e)
                {
                    return StatusCode(500, e);
                }
            }
            else
            {
                return Unauthorized("Greška, niste vlasnik oglasa.");
            }
        }

        [HttpPut]
        [Route("add_advert_image")]
        public ActionResult AddAdvertImage(uint advertId, [FromBody] FileData image)
        {
            if (JwtHelper.TokenUnverified(currentUserId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            if (image == null) { return BadRequest("Object must not be null!"); }
            try
            {
                //FileData imageData = JsonSerializer.Deserialize<FileData>(image);
                return TryEditAdvert(advertId, () => img.AddAdvertImage(advertId, image));
            }
            catch (Exception e)
            {
                return BadRequest(e);
            }
        }

        [HttpPut]
        [Route("add_advert_images")]
        public ActionResult AddAdvertImages(uint advertId,[FromBody] IEnumerable<FileData> images)
        {
            if (JwtHelper.TokenUnverified(currentUserId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            //if (images == null) { return BadRequest("Object must not be null!"); }
            try
            {
                //List<FileData> imageData = JsonSerializer.Deserialize<List<FileData>>(images);
                return TryEditAdvert(advertId, () => img.AddAdvertImages(advertId, images));
            }
            catch (Exception e)
            {
                return BadRequest(e);
            }
            
        }

        [HttpDelete]
        [Route("delete_advert_image")]
        public ActionResult DeleteAdvertImage(uint advertId, string imageName = null)
        {
            if (JwtHelper.TokenUnverified(currentUserId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return TryEditAdvert(advertId, () => img.DeleteAdvertImage(advertId, imageName));
        }

        [HttpDelete]
        [Route("delete_advert_images")]
        public ActionResult DeleteAdvertImages(uint advertId)
        {
            if (JwtHelper.TokenUnverified(currentUserId, Request))
                return Unauthorized(AdvertController.unAuthMsg);

            return TryEditAdvert(advertId, () => img.DeleteAdvertImage(advertId));
        }
    }
}
