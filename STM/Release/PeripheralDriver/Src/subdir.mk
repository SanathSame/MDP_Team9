################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (10.3-2021.10)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
../PeripheralDriver/Src/icm20948.c \
../PeripheralDriver/Src/oled.c \
../PeripheralDriver/Src/tm_stm32_ahrs_imu.c \
../PeripheralDriver/Src/tm_stm32_gpio.c \
../PeripheralDriver/Src/tm_stm32_i2c.c 

OBJS += \
./PeripheralDriver/Src/icm20948.o \
./PeripheralDriver/Src/oled.o \
./PeripheralDriver/Src/tm_stm32_ahrs_imu.o \
./PeripheralDriver/Src/tm_stm32_gpio.o \
./PeripheralDriver/Src/tm_stm32_i2c.o 

C_DEPS += \
./PeripheralDriver/Src/icm20948.d \
./PeripheralDriver/Src/oled.d \
./PeripheralDriver/Src/tm_stm32_ahrs_imu.d \
./PeripheralDriver/Src/tm_stm32_gpio.d \
./PeripheralDriver/Src/tm_stm32_i2c.d 


# Each subdirectory must supply rules for building sources it contributes
PeripheralDriver/Src/%.o PeripheralDriver/Src/%.su: ../PeripheralDriver/Src/%.c PeripheralDriver/Src/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m4 -std=gnu11 -DUSE_HAL_DRIVER -DSTM32F407xx -c -I../Core/Inc -I../Drivers/STM32F4xx_HAL_Driver/Inc -I../Drivers/STM32F4xx_HAL_Driver/Inc/Legacy -I../Drivers/CMSIS/Device/ST/STM32F4xx/Include -I../Drivers/CMSIS/Include -I../Middlewares/Third_Party/FreeRTOS/Source/include -I../Middlewares/Third_Party/FreeRTOS/Source/CMSIS_RTOS_V2 -I../Middlewares/Third_Party/FreeRTOS/Source/portable/GCC/ARM_CM4F -I"C:/Users/Jeff Chua/Desktop/Year 3 Sem 2/CE3004 Multidisciplinary Design Project/MDP_Team9/STM/PeripheralDriver/Inc" -Os -ffunction-sections -fdata-sections -Wall -fstack-usage -MMD -MP -MF"$(@:%.o=%.d)" -MT"$@" --specs=nano.specs -mfpu=fpv4-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

clean: clean-PeripheralDriver-2f-Src

clean-PeripheralDriver-2f-Src:
	-$(RM) ./PeripheralDriver/Src/icm20948.d ./PeripheralDriver/Src/icm20948.o ./PeripheralDriver/Src/icm20948.su ./PeripheralDriver/Src/oled.d ./PeripheralDriver/Src/oled.o ./PeripheralDriver/Src/oled.su ./PeripheralDriver/Src/tm_stm32_ahrs_imu.d ./PeripheralDriver/Src/tm_stm32_ahrs_imu.o ./PeripheralDriver/Src/tm_stm32_ahrs_imu.su ./PeripheralDriver/Src/tm_stm32_gpio.d ./PeripheralDriver/Src/tm_stm32_gpio.o ./PeripheralDriver/Src/tm_stm32_gpio.su ./PeripheralDriver/Src/tm_stm32_i2c.d ./PeripheralDriver/Src/tm_stm32_i2c.o ./PeripheralDriver/Src/tm_stm32_i2c.su

.PHONY: clean-PeripheralDriver-2f-Src

