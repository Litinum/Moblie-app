using Microsoft.Extensions.Configuration;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using WebAPI.Models;

namespace WebAPI.Services
{
    public class SkuciSeImageService
    {
        private FileService fs;
        private IConfiguration config;

        private readonly string basePath;
        private readonly string advertPath = "Advert";
        private readonly string userPath = "User";
        private readonly string defaultUserImagePath;

        public string GetUserPath(uint userId)
        {
            return $"{basePath}\\{userPath}\\{userId}";
        }

        public string GetDefaultUserImagePath()
        {
            return $"{basePath}\\{userPath}\\{defaultUserImagePath}";
        }

        public string GetAdvertPath(uint advertId)
        {
            return $"{basePath}\\{advertPath}\\{advertId}";
        }

        public SkuciSeImageService(IConfiguration _config)
        {
            config = _config;
            var filesConfig = config.GetSection("FileHosting");
            basePath = filesConfig.GetValue("ImagePath", @".\Files\Images_Default");
            defaultUserImagePath = filesConfig.GetValue("DefaultUserImage", "default_user.jpg");
            fs = new FileService(basePath, new string[] { advertPath, userPath });
        }

        public FileData GetUserImage(uint userId)
        {
            FileData fd;
            try
            {
                fd = fs.ReadFile($"{userPath}\\{userId}");
            }
            catch
            {
                fd = fs.ReadFile($"{userPath}", $"{defaultUserImagePath}");
            }
            return fd;
        }

        public void SetUserImage(uint userId, FileData image) {
            fs.WriteFile($"{userPath}\\{userId}", image);
        }

        public void DeleteUserImage(uint userId)
        {
            fs.DeleteAllFiles($"{userPath}\\{userId}");
        }

        public void AddAdvertImage(uint advertId, FileData image)
        {
            fs.WriteFile($"{advertPath}\\{advertId}", image);
        }

        public void AddAdvertImages(uint advertId, IEnumerable<FileData> images)
        {
            fs.WriteFolder($"{advertPath}\\{advertId}", images);
        }

        public void DeleteAdvertImage(uint advertId, string imageName = null)
        {
            fs.DeleteFile($"{advertPath}\\{advertId}", imageName);
        }

        public void DeleteAdvertImages(uint advertId)
        {
            fs.DeleteAllFiles($"{advertPath}\\{advertId}");
        }

        public List<FileData> GetAdvertImages(uint advertId)
        {
            return fs.ReadFolder($"{userPath}\\{advertId}");
        }
    }
}
