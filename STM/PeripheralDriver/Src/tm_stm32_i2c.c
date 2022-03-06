/**
 * |----------------------------------------------------------------------
 * | Copyright (c) 2016 Tilen MAJERLE
 * |
 * | Permission is hereby granted, free of charge, to any person
 * | obtaining a copy of this software and associated documentation
 * | files (the "Software"), to deal in the Software without restriction,
 * | including without limitation the rights to use, copy, modify, merge,
 * | publish, distribute, sublicense, and/or sell copies of the Software,
 * | and to permit persons to whom the Software is furnished to do so,
 * | subject to the following conditions:
 * |
 * | The above copyright notice and this permission notice shall be
 * | included in all copies or substantial portions of the Software.
 * |
 * | THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * | EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * | OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * | AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * | HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * | WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * | FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * | OTHER DEALINGS IN THE SOFTWARE.
 * |----------------------------------------------------------------------
 */
#include "tm_stm32_i2c.h"
#include "stm32f4xx_hal_i2c.h"

/* Timeout value */
#define I2C_TIMEOUT_VALUE              1000

TM_I2C_Result_t TM_I2C_Read(I2C_HandleTypeDef* Handle, uint8_t device_address, uint8_t register_address, uint8_t* data) {

	/* Send address */
	if (HAL_I2C_Master_Transmit(Handle, (uint16_t)device_address << 1, &register_address, 1, 1000) != HAL_OK) {
		/* Check error */
		if (HAL_I2C_GetError(Handle) != HAL_I2C_ERROR_AF) {

		}

		/* Return error */
		return TM_I2C_Result_Error;
	}

	/* Receive multiple byte */
	if (HAL_I2C_Master_Receive(Handle, device_address << 1, data, 1, 1000) != HAL_OK) {
		/* Check error */
		if (HAL_I2C_GetError(Handle) != HAL_I2C_ERROR_AF) {

		}

		/* Return error */
		return TM_I2C_Result_Error;
	}

	/* Return OK */
	return TM_I2C_Result_Ok;
}

TM_I2C_Result_t TM_I2C_ReadMulti(I2C_HandleTypeDef* Handle, uint8_t device_address, uint8_t register_address, uint8_t* data, uint16_t count) {

	/* Send register address */
	if (HAL_I2C_Master_Transmit(Handle, (uint16_t)device_address << 1, &register_address, 1, 1000) != HAL_OK) {
		/* Check error */
		if (HAL_I2C_GetError(Handle) != HAL_I2C_ERROR_AF) {

		}

		/* Return error */
		return TM_I2C_Result_Error;
	}

	/* Receive multiple byte */
	if (HAL_I2C_Master_Receive(Handle, device_address << 1, data, count, 1000) != HAL_OK) {
		/* Check error */
		if (HAL_I2C_GetError(Handle) != HAL_I2C_ERROR_AF) {

		}

		/* Return error */
		return TM_I2C_Result_Error;
	}

	/* Return OK */
	return TM_I2C_Result_Ok;
}

TM_I2C_Result_t TM_I2C_Write(I2C_HandleTypeDef* Handle, uint8_t device_address, uint8_t register_address, uint8_t data) {
	uint8_t d[2];

	/* Format array to send */
	d[0] = register_address;
	d[1] = data;

	/* Try to transmit via I2C */
	if (HAL_I2C_Master_Transmit(Handle, (uint16_t)device_address << 1, (uint8_t *)d, 2, 1000) != HAL_OK) {
		/* Check error */
		if (HAL_I2C_GetError(Handle) != HAL_I2C_ERROR_AF) {

		}

		/* Return error */
		return TM_I2C_Result_Error;
	}

	/* Return OK */
	return TM_I2C_Result_Ok;
}

TM_I2C_Result_t TM_I2C_WriteMulti(I2C_HandleTypeDef* Handle, uint8_t device_address, uint16_t register_address, uint8_t* data, uint16_t count) {

	/* Try to transmit via I2C */
	if (HAL_I2C_Mem_Write(Handle, device_address << 1, register_address, register_address > 0xFF ? I2C_MEMADD_SIZE_16BIT : I2C_MEMADD_SIZE_8BIT, data, count, 1000) != HAL_OK) {
		/* Check error */
		if (HAL_I2C_GetError(Handle) != HAL_I2C_ERROR_AF) {

		}

		/* Return error */
		return TM_I2C_Result_Error;
	}

	/* Return OK */
	return TM_I2C_Result_Ok;
}

TM_I2C_Result_t TM_I2C_IsDeviceConnected(I2C_HandleTypeDef* Handle, uint8_t device_address) {

	/* Check if device is ready for communication */
	if (HAL_I2C_IsDeviceReady(Handle, device_address, 2, 5) != HAL_OK) {
		/* Return error */
		return TM_I2C_Result_Error;
	}

	/* Return OK */
	return TM_I2C_Result_Ok;
}
